package com.mintanable.notethepad.feature_note.presentation.modify

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.mintanable.notethepad.core.common.CheckboxConvertors
import com.mintanable.notethepad.core.common.utils.readAndProcessImage
import com.mintanable.notethepad.core.richtext.compose.RichTextAnnotator
import com.mintanable.notethepad.core.richtext.engine.SpanAdjustmentEngine
import com.mintanable.notethepad.core.richtext.model.RichTextDocument
import com.mintanable.notethepad.core.richtext.serializer.RichTextSerializer
import com.mintanable.notethepad.database.db.entity.Attachment
import com.mintanable.notethepad.database.db.entity.AttachmentType
import com.mintanable.notethepad.core.model.note.CheckboxItem
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.core.richtext.model.SpanType
import com.mintanable.notethepad.core.richtext.utils.TextUtils
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.database.db.util.AudioMetadataProvider
import com.mintanable.notethepad.feature_ai.domain.use_cases.GetAutoTagsUseCase
import com.mintanable.notethepad.feature_ai.domain.use_cases.StartLiveTransctiption
import com.mintanable.notethepad.feature_ai.domain.use_cases.StopLiveTranscription
import com.mintanable.notethepad.feature_ai.domain.use_cases.AnalyzeImageUseCase
import com.mintanable.notethepad.feature_ai.domain.use_cases.QueryImageUseCase
import com.mintanable.notethepad.feature_ai.domain.use_cases.TranscribeAudioFileUseCase
import com.mintanable.notethepad.feature_note.domain.repository.AudioRecorder
import com.mintanable.notethepad.feature_note.domain.repository.MediaPlayer
import com.mintanable.notethepad.feature_note.domain.repository.ReminderScheduler
import com.mintanable.notethepad.feature_note.domain.use_case.fileio.FileIOUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.notes.NoteUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.permissions.PermissionUsecases
import com.mintanable.notethepad.feature_note.domain.use_case.tags.TagUseCases
import com.mintanable.notethepad.feature_note.presentation.AddEditNoteUiState
import com.mintanable.notethepad.feature_note.presentation.notes.util.AttachmentHelper
import com.mintanable.notethepad.permissions.DeniedType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AddEditNoteViewModel @Inject constructor(
    private val noteUseCases: NoteUseCases,
    savedStateHandle: SavedStateHandle,
    private val permissionUsecases: PermissionUsecases,
    private val audioRecorder: AudioRecorder,
    private val fileIOUseCases: FileIOUseCases,
    private val mediaPlayer: MediaPlayer,
    private val audioMetadataProvider: AudioMetadataProvider,
    private val reminderScheduler: ReminderScheduler,
    tagUseCases: TagUseCases,
    private val getAutoTagsUseCase: GetAutoTagsUseCase,
    private val startLiveTransctiption: StartLiveTransctiption,
    private val stopLiveTransctiptions: StopLiveTranscription,
    private val transcribeAudioFileUseCase: TranscribeAudioFileUseCase,
    private val analyzeImageUseCase: AnalyzeImageUseCase,
    private val queryImageUseCase: QueryImageUseCase,
    @ApplicationContext private val appContext: Context
): ViewModel(){

    private val passedNoteId: Long = savedStateHandle.get<Long>("noteId") ?: -1L
    private val passedReminderTime: Long = savedStateHandle.get<Long>("reminderTime") ?: -1L
    private val passedInitialTitle: String = savedStateHandle.get<String>("initialTitle") ?: ""
    private val isEditMode = passedNoteId != -1L
    private var currentNoteId: Long = 0L
    private var currentRecordingFile: File? = null
    private val imageSuggestionsCache = mutableMapOf<String, List<String>>()
    private var cachedImageBytes: ByteArray? = null

    private val _uiState = MutableStateFlow(
        AddEditNoteUiState(
            noteColor =
                if (isEditMode) -1 else NoteColors.colors.random().toArgb()
        )
    )
    val uiState: StateFlow<AddEditNoteUiState> = combine(
        _uiState,
        mediaPlayer.mediaState,
        audioRecorder.amplitude
    ) { state, mediaState, amplitude ->
        state.copy(mediaState = mediaState, recordingAmplitude = amplitude)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddEditNoteUiState()
    )

    val existingTags = tagUseCases.getAllTags().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val videoPlayerEngine: ExoPlayer? = mediaPlayer.exoPlayer

    init {
        loadNote(passedNoteId)
        if (!isEditMode) {
            if (passedReminderTime > 0L) {
                _uiState.update { it.copy(reminderTime = passedReminderTime) }
            }
            if (passedInitialTitle.isNotBlank()) {
                _uiState.update { it.copy(
                    titleState = it.titleState.copy(
                        richText = RichTextDocument(rawText = passedInitialTitle),
                        isHintVisible = false
                    )
                ) }
            }
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            noteUseCases.getDetailedNote(id)?.also { detailedNote ->
                currentNoteId = detailedNote.id
                val richText = RichTextSerializer.deserialize(detailedNote.content)
                val annotated = RichTextAnnotator.toAnnotatedString(richText)
                _uiState.update {
                    it.copy(
                        titleState = it.titleState.copy(
                            richText = RichTextDocument(rawText = detailedNote.title),
                            isHintVisible = false
                        ),
                        contentState = it.contentState.copy(
                            richText = richText,
                            isHintVisible = false
                        ),
                        contentTextFieldValue = TextFieldValue(annotatedString = annotated),
                        noteColor = detailedNote.color,
                        attachedImages = detailedNote.imageUris.map { imgString -> imgString.toUri() },
                        attachedAudios = detailedNote.audioAttachments,
                        reminderTime = detailedNote.reminderTime,
                        checkListItems = detailedNote.checkListItems,
                        isCheckboxListAvailable = detailedNote.isCheckboxListAvailable,
                        tagEntities = detailedNote.tagEntities
                    )
                }
            }
        }
    }

    fun onEvent(event:AddEditNoteEvent){
        Log.d("AddEditNoteViewModel", "event: $event")
        when(event){
            is AddEditNoteEvent.EnteredTitle -> {
                _uiState.update { it.copy(
                    titleState = it.titleState.copy(
                        richText = RichTextDocument(rawText = event.value)
                    )
                )}
            }
            is AddEditNoteEvent.ChangeTitleFocus -> {
                _uiState.update { it.copy(
                    titleState = it.titleState.copy(
                        isFocused = event.focusState.isFocused,
                        isHintVisible = !event.focusState.isFocused && it.titleState.richText.rawText.isBlank()
                    )
                )}
            }
            is AddEditNoteEvent.EnteredContent -> {
                val newText = event.value.text
                var newCursorPos = event.value.selection.start

                val oldState = _uiState.value
                val oldDoc = oldState.contentState.richText

                if (newText == oldDoc.rawText) {
                    // Cursor/selection moved without text change — sync pending from current line
                    val derived = derivePendingFromCursor(oldDoc, newCursorPos)
                    _uiState.update { it.copy(
                        contentTextFieldValue = event.value.copy(annotatedString = it.contentTextFieldValue.annotatedString),
                        pendingBlockType = derived.block,
                        pendingBullet = derived.bullet,
                        pendingStyles = derived.inline,
                        activeContentStyles = buildActiveStyles(derived.block, derived.bullet, derived.inline)
                    )}
                } else {
                    val changeStart = TextUtils.findTextChangeStart(oldDoc.rawText, newText)
                    val changeEnd = TextUtils.findTextChangeEnd(oldDoc.rawText, newText, changeStart)
                    var newDoc = SpanAdjustmentEngine.adjustForTextChange(oldDoc, newText, changeStart, changeEnd)

                    val isInsertion = newText.length > oldDoc.rawText.length
                    if (isInsertion) {
                        val insertLen = newText.length - oldDoc.rawText.length
                        val insertEnd = changeStart + insertLen
                        val insertedText = newText.substring(changeStart, insertEnd)

                        // Bullet continuation on Enter
                        if (oldState.pendingBullet && '\n' in insertedText) {
                            for (i in insertedText.indices) {
                                if (insertedText[i] == '\n') {
                                    val newlinePos = changeStart + i
                                    val (bulletDoc, extra) = SpanAdjustmentEngine.continueBulletAfterEnter(newDoc, newlinePos)
                                    newDoc = bulletDoc
                                    newCursorPos += extra
                                }
                            }
                        }

                        // Ensure BULLET span covers the line(s) if pendingBullet is active
                        if (oldState.pendingBullet) {
                            newDoc = SpanAdjustmentEngine.ensureBullet(newDoc, changeStart, changeStart + 1)
                            if ('\n' in insertedText) {
                                val allLineRanges = SpanAdjustmentEngine.getLineRanges(newDoc.rawText, changeStart, newCursorPos)
                                for ((ls, le) in allLineRanges) {
                                    if (le > ls) {
                                        newDoc = SpanAdjustmentEngine.ensureBullet(newDoc, ls, le)
                                    }
                                }
                            }
                        }

                        // Ensure pending block type covers the line(s) containing the insertion
                        if (oldState.pendingBlockType != null) {
                            newDoc = SpanAdjustmentEngine.ensureBlockType(newDoc, oldState.pendingBlockType, changeStart, changeStart + 1)
                            if ('\n' in insertedText) {
                                val allLineRanges = SpanAdjustmentEngine.getLineRanges(newDoc.rawText, changeStart, newCursorPos)
                                for ((ls, le) in allLineRanges) {
                                    if (le > ls) {
                                        newDoc = SpanAdjustmentEngine.ensureBlockType(newDoc, oldState.pendingBlockType, ls, le)
                                    }
                                }
                            }
                        }

                        // Apply pending inline styles to the user's inserted chars
                        for (style in oldState.pendingStyles) {
                            if (!newDoc.isActiveAt(style, changeStart, insertEnd)) {
                                newDoc = SpanAdjustmentEngine.toggleSpan(newDoc, style, changeStart, insertEnd)
                            }
                        }
                        // Remove inline styles that are active on inserted chars but NOT in pendingStyles
                        val inlineTypes = listOf(SpanType.BOLD, SpanType.ITALIC, SpanType.UNDERLINE)
                        for (style in inlineTypes) {
                            if (style !in oldState.pendingStyles && newDoc.isActiveAt(style, changeStart, insertEnd)) {
                                newDoc = SpanAdjustmentEngine.toggleSpan(newDoc, style, changeStart, insertEnd)
                            }
                        }
                    }

                    val annotated = RichTextAnnotator.toAnnotatedString(newDoc)
                    val finalCursor = newCursorPos.coerceIn(0, newDoc.rawText.length)
                    val newTFV = TextFieldValue(
                        annotatedString = annotated,
                        selection = TextRange(finalCursor)
                    )

                    // After deletion, derive pending from current line; after insertion, keep existing pending
                    val newPendingBlock: SpanType?
                    val newPendingBullet: Boolean
                    val newPendingInline: Set<SpanType>
                    if (!isInsertion) {
                        val derived = derivePendingFromCursor(newDoc, finalCursor)
                        newPendingBlock = derived.block
                        newPendingBullet = derived.bullet
                        newPendingInline = derived.inline
                    } else {
                        newPendingBlock = oldState.pendingBlockType
                        newPendingBullet = oldState.pendingBullet
                        newPendingInline = oldState.pendingStyles
                    }

                    _uiState.update { it.copy(
                        contentState = it.contentState.copy(
                            richText = newDoc,
                            isHintVisible = newDoc.rawText.isBlank() && !it.contentState.isFocused
                        ),
                        contentTextFieldValue = newTFV,
                        pendingBlockType = newPendingBlock,
                        pendingBullet = newPendingBullet,
                        pendingStyles = newPendingInline,
                        activeContentStyles = buildActiveStyles(newPendingBlock, newPendingBullet, newPendingInline)
                    )}
                }
            }
            is AddEditNoteEvent.ChangeContentFocus -> {
                val focused = event.focusState.isFocused
                _uiState.update { it.copy(
                    contentState = it.contentState.copy(
                        isFocused = focused,
                        isHintVisible = !focused && it.contentState.richText.rawText.isBlank()
                    ),
                    isRichTextBarActive = if (!focused) false else it.isRichTextBarActive,
                    pendingBlockType = if (!focused) null else it.pendingBlockType,
                    pendingBullet = if (!focused) false else it.pendingBullet,
                    pendingStyles = if (!focused) emptySet() else it.pendingStyles
                )}
            }
            is AddEditNoteEvent.ApplyContentFormat -> {
                val oldState = _uiState.value
                val sel = oldState.contentTextFieldValue.selection
                val hasSelection = sel.start < sel.end

                if (event.type == SpanType.BULLET) {
                    // ── BULLET toggle (independent of H1/H2/P) ──
                    val doc = oldState.contentState.richText
                    val lineRanges = SpanAdjustmentEngine.getLineRanges(doc.rawText, sel.start, sel.end)
                    val isEmptyLine = doc.rawText.isEmpty() || lineRanges.all { (ls, le) -> ls == le }

                    // Check if the current line(s) already have a BULLET span
                    val currentlyBullet = lineRanges.any { (ls, le) ->
                        le > ls && doc.isActiveAt(SpanType.BULLET, ls, le)
                    }
                    // Toggle: if line has bullet, turn off; if not, turn on
                    val newBulletIntent = if (isEmptyLine) !oldState.pendingBullet else !currentlyBullet

                    if (isEmptyLine) {
                        // Empty line: toggle pending only, upcoming text will get bullet
                        _uiState.update { it.copy(
                            pendingBullet = newBulletIntent,
                            activeContentStyles = buildActiveStyles(it.pendingBlockType, newBulletIntent, it.pendingStyles)
                        )}
                    } else {
                        var newDoc = doc
                        var cursorDelta = 0
                        val cursorPos = sel.start

                        // Use expanded line ranges for span operations
                        val expandedStart = lineRanges.first().first
                        val expandedEnd = lineRanges.last().second

                        if (newBulletIntent) {
                            // ── Turning BULLET ON ──
                            // Add BULLET span to the line(s)
                            val spans = SpanAdjustmentEngine.addSpanToRange(
                                newDoc.spans, SpanType.BULLET, expandedStart, expandedEnd
                            )
                            newDoc = RichTextDocument.of(newDoc.rawText, spans)

                            // Insert "• " prefixes
                            val (prefixInserted, delta) = SpanAdjustmentEngine.insertBulletPrefixes(newDoc, lineRanges, cursorPos)
                            newDoc = prefixInserted
                            cursorDelta += delta

                            // Re-ensure BULLET covers expanded range after prefix insertion
                            val updatedLines = SpanAdjustmentEngine.getLineRanges(
                                newDoc.rawText,
                                (expandedStart + cursorDelta).coerceAtLeast(0),
                                (expandedEnd + cursorDelta).coerceAtLeast(0)
                            )
                            val uStart = updatedLines.first().first
                            val uEnd = updatedLines.last().second
                            if (uEnd > uStart) {
                                newDoc = SpanAdjustmentEngine.ensureBullet(newDoc, uStart, uEnd)
                            }
                        } else {
                            // ── Turning BULLET OFF ──
                            // Remove BULLET span from the line(s)
                            val spans = SpanAdjustmentEngine.removeSpanFromRange(
                                newDoc.spans, SpanType.BULLET, expandedStart, expandedEnd
                            )
                            newDoc = RichTextDocument.of(newDoc.rawText, spans)

                            // Remove "• " prefixes
                            val (prefixRemoved, delta) = SpanAdjustmentEngine.removeBulletPrefixes(newDoc, lineRanges, cursorPos)
                            newDoc = prefixRemoved
                            cursorDelta += delta
                        }

                        val annotated = RichTextAnnotator.toAnnotatedString(newDoc)
                        val newCursorPos = (cursorPos + cursorDelta).coerceIn(0, newDoc.rawText.length)
                        val newSelEnd = if (hasSelection) (sel.end + cursorDelta).coerceIn(0, newDoc.rawText.length) else newCursorPos

                        _uiState.update { it.copy(
                            contentState = it.contentState.copy(richText = newDoc),
                            contentTextFieldValue = TextFieldValue(
                                annotatedString = annotated,
                                selection = TextRange(newCursorPos, newSelEnd)
                            ),
                            pendingBullet = newBulletIntent,
                            activeContentStyles = buildActiveStyles(it.pendingBlockType, newBulletIntent, it.pendingStyles)
                        )}
                    }
                } else if (event.type in SpanAdjustmentEngine.BLOCK_TYPES) {
                    // ── Block type toggle (H1/H2/P) — does NOT touch BULLET ──
                    val doc = oldState.contentState.richText
                    val lineRangesForCheck = SpanAdjustmentEngine.getLineRanges(doc.rawText, sel.start, sel.end)
                    val isEmptyLine = doc.rawText.isEmpty() || lineRangesForCheck.all { (ls, le) -> ls == le }

                    if (isEmptyLine) {
                        val newBlock = if (event.type == oldState.pendingBlockType) null else event.type
                        _uiState.update { it.copy(
                            pendingBlockType = newBlock,
                            activeContentStyles = buildActiveStyles(newBlock, it.pendingBullet, it.pendingStyles)
                        )}
                    } else {
                        var newDoc = doc
                        val adjStart = sel.start
                        val adjEnd = if (hasSelection) sel.end else adjStart
                        newDoc = SpanAdjustmentEngine.applyBlockTypeToLines(newDoc, event.type, adjStart, adjEnd)

                        val annotated = RichTextAnnotator.toAnnotatedString(newDoc)
                        val derived = derivePendingFromCursor(newDoc, sel.start)

                        _uiState.update { it.copy(
                            contentState = it.contentState.copy(richText = newDoc),
                            contentTextFieldValue = TextFieldValue(
                                annotatedString = annotated,
                                selection = sel
                            ),
                            pendingBlockType = derived.block,
                            activeContentStyles = buildActiveStyles(derived.block, it.pendingBullet, it.pendingStyles)
                        )}
                    }
                } else {
                    // ── Inline style toggle (BOLD/ITALIC/UNDERLINE) ──
                    if (hasSelection) {
                        val doc = SpanAdjustmentEngine.toggleSpan(
                            oldState.contentState.richText, event.type, sel.start, sel.end
                        )
                        val annotated = RichTextAnnotator.toAnnotatedString(doc)
                        val derived = derivePendingFromCursor(doc, sel.start)
                        val newBlock = derived.block
                        val newBullet = derived.bullet
                        val newInline = derived.inline
                        _uiState.update { it.copy(
                            contentState = it.contentState.copy(richText = doc),
                            contentTextFieldValue = it.contentTextFieldValue.copy(annotatedString = annotated),
                            pendingBlockType = newBlock,
                            pendingBullet = newBullet,
                            pendingStyles = newInline,
                            activeContentStyles = buildActiveStyles(newBlock, newBullet, newInline)
                        )}
                    } else {
                        val newInline = if (event.type in oldState.pendingStyles)
                            oldState.pendingStyles - event.type
                        else
                            oldState.pendingStyles + event.type
                        _uiState.update { it.copy(
                            pendingStyles = newInline,
                            activeContentStyles = buildActiveStyles(it.pendingBlockType, it.pendingBullet, newInline)
                        )}
                    }
                }
            }
            is AddEditNoteEvent.ToggleRichTextBar -> {
                _uiState.update { it.copy(isRichTextBarActive = !it.isRichTextBarActive) }
            }
            is AddEditNoteEvent.ChangeColor -> {
                _uiState.update { it.copy(noteColor = event.color) }
            }
            is AddEditNoteEvent.SaveNote -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isSaving = true) }
                    saveNote(currentNoteId)
                        .onSuccess {
                            if(currentNoteId != 0L) { rescheduleReminder(currentNoteId) }
                            _uiState.update { it.copy(isSaving = false, zoomedImageUri = null) }
                            _eventFlow.emit(UiEvent.SaveNote)
                        }
                        .onFailure { e ->
                            _uiState.update { it.copy(isSaving = false) }
                            _eventFlow.emit(UiEvent.ShowSnackbar(e.message ?: "Save Failed"))
                        }
                }

            }
            is AddEditNoteEvent.MakeCopy -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isSaving = true) }
                    saveNote(0L)
                        .onSuccess { id ->
                            _uiState.update { it.copy(isSaving = false, zoomedImageUri = null) }
                            _eventFlow.emit(UiEvent.MakeCopy(id))
                        }
                        .onFailure { e ->
                            _uiState.update { it.copy(isSaving = false) }
                            _eventFlow.emit(UiEvent.ShowSnackbar(e.message ?: "Save Failed"))
                        }
                }
            }
            is AddEditNoteEvent.DeleteNote -> {
                deleteNote()
            }
            is AddEditNoteEvent.PinNote -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isSaving = true) }
                    saveNote(currentNoteId)
                        .onSuccess { id ->
                            _uiState.update { it.copy(isSaving = false) }
                            _eventFlow.emit(UiEvent.RequestWidgetPin(id))
                        }
                        .onFailure {
                            _uiState.update { it.copy(isSaving = false) }
                            _eventFlow.emit(UiEvent.ShowSnackbar("Save Failed, can't pin to homescreen"))
                        }
                }
            }
            is AddEditNoteEvent.AttachImage -> {
                _uiState.update { it.copy(
                    attachedImages = if (it.attachedImages.contains(event.uri)) it.attachedImages else it.attachedImages + event.uri
                )}
            }
            is AddEditNoteEvent.RemoveImage -> {
                _uiState.update { it.copy(attachedImages = it.attachedImages - event.uri) }
                viewModelScope.launch { fileIOUseCases.deleteFiles(listOf(event.uri.toString())) }
            }
            is AddEditNoteEvent.AttachVideo -> {
                _uiState.update { it.copy(
                    attachedImages = if (it.attachedImages.contains(event.uri)) it.attachedImages else it.attachedImages + event.uri
                )}
            }
            is AddEditNoteEvent.RemoveAudio -> {
                viewModelScope.launch {
                    _uiState.update { state ->
                        state.copy(
                            attachedAudios = state.attachedAudios.filterNot { it.uri == event.uri }
                        )
                    }
                    if (mediaPlayer.mediaState.first().currentUri == event.uri) {
                        mediaPlayer.stop()
                    }
                }
            }
            is AddEditNoteEvent.ToggleAudioRecording -> {
                handleRecording(event.enableLiveTranscription)
            }
            is AddEditNoteEvent.DismissDialogs -> {
                _uiState.update { it.copy(
                    showCameraRationale = false,
                    showMicrophoneRationale = false,
                    settingsDeniedType = null,
                    showAddNewTagDialog = false
                )}
            }
            is AddEditNoteEvent.UpdateSheetType -> {
                _uiState.update { it.copy(currentSheetType = event.sheetType) }
            }
            is AddEditNoteEvent.ToggleZoom -> {
                val attachmentType = AttachmentHelper.getAttachmentType(appContext, event.uri)
                if (attachmentType == AttachmentType.VIDEO) {
                    mediaPlayer.playPause(event.uri)
                }
                _uiState.update { it.copy(zoomedImageUri = event.uri) }

                if (attachmentType != AttachmentType.VIDEO) {
                    analyzeImage(event.uri)
                }
            }
            is AddEditNoteEvent.UpdateNowPlaying -> {
                mediaPlayer.playPause(event.uri.toUri())
            }
            is AddEditNoteEvent.StopMedia -> {
                mediaPlayer.stop()
                _uiState.update { it.copy(
                    zoomedImageUri = null,
                    imageSuggestions = emptyList(),
                    imageQueryResult = "",
                    isImageQueryLoading = false,
                    isAnalyzingImage = false
                ) }
            }
            is AddEditNoteEvent.SetReminder -> {
                _uiState.update { it.copy(reminderTime = event.timestamp, showAlarmPermissionRationale = false, showDataAndTimePicker = false) }
            }
            is AddEditNoteEvent.CancelReminder -> {
                _uiState.update { it.copy(reminderTime = -1, showAlarmPermissionRationale = false, showDataAndTimePicker = false) }
                if(currentNoteId != 0L) { reminderScheduler.cancel(currentNoteId) }
            }
            is AddEditNoteEvent.CheckAlarmPermission -> {
                checkExactAlarmPermission()
            }
            is AddEditNoteEvent.DismissReminder -> {
                _uiState.update { it.copy(showAlarmPermissionRationale = false, showDataAndTimePicker = false) }
            }
            is AddEditNoteEvent.ToggleCheckbox -> {
                if(uiState.value.isCheckboxListAvailable) {
                    val textContent = CheckboxConvertors.checkboxesToContentString(uiState.value.checkListItems)
                    val richText = RichTextDocument(rawText = textContent)
                    val annotated = RichTextAnnotator.toAnnotatedString(richText)
                    _uiState.update {
                        it.copy(
                            isCheckboxListAvailable = false,
                            checkListItems = emptyList(),
                            contentState = it.contentState.copy(richText = richText, isHintVisible = false),
                            contentTextFieldValue = TextFieldValue(annotatedString = annotated)
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isCheckboxListAvailable = true,
                            checkListItems = CheckboxConvertors.stringToCheckboxes(it.contentState.richText.rawText),
                            contentState = it.contentState.copy(
                                richText = RichTextDocument.EMPTY,
                                isHintVisible = false
                            ),
                            contentTextFieldValue = TextFieldValue()
                        )
                    }
                }
            }

            is AddEditNoteEvent.UpdateCheckList -> {
                _uiState.update { it.copy(checkListItems = event.list) }
            }

            is AddEditNoteEvent.AddChecklistItem -> {
                _uiState.update { currentState ->
                    val currentItems = currentState.checkListItems
                    val targetIndex = currentItems.indexOfFirst { it.id == event.previousCheckItem.id }

                    val newList = if (targetIndex != -1) {
                        currentItems.toMutableList().apply {
                            add(targetIndex + 1,CheckboxItem(text = "", isChecked = event.previousCheckItem.isChecked))
                        }
                    } else {
                        currentItems + CheckboxItem(text = "", isChecked = event.previousCheckItem.isChecked)
                    }

                    currentState.copy(checkListItems = newList)
                }
            }

            is AddEditNoteEvent.ShowLabelDialog -> {
                _uiState.update { it.copy(showAddNewTagDialog = true) }
            }

            is AddEditNoteEvent.InsertLabel -> {
                _uiState.update { currentState ->
                    val newTagEntities = if (currentState.tagEntities.map { it.tagName }.contains(event.tagName)) {
                        currentState.tagEntities
                    } else {
                        currentState.tagEntities + TagEntity(event.tagName)
                    }
                    val newSuggestedTags = if(currentState.suggestedTags.contains(event.tagName)){
                        currentState.suggestedTags - event.tagName
                    }else{
                        currentState.suggestedTags
                    }
                    currentState.copy(tagEntities = newTagEntities, showAddNewTagDialog = false, suggestedTags = newSuggestedTags)
                }
            }
            is AddEditNoteEvent.DeleteLabel -> {
                _uiState.update { currentState ->
                    val newList = currentState.tagEntities.filterNot { it.tagName == event.tagName }
                    currentState.copy(tagEntities = newList, showAddNewTagDialog = false)
                }
            }

            is AddEditNoteEvent.ShowSuggestions -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isTagSuggestionLoading = true) }
                    getAutoTagsUseCase(
                        title = uiState.value.titleState.richText.rawText,
                        content = uiState.value.contentState.richText.rawText
                    )
                        .onSuccess { suggestions ->
                            _uiState.update { it.copy(isTagSuggestionLoading = false, suggestedTags = suggestions) } }
                        .onFailure { exeption ->
                            _uiState.update { it.copy(isTagSuggestionLoading = false) }
                            _eventFlow.emit(UiEvent.ShowSnackbar(exeption.message ?: "Save Failed"))
                        }
                }
            }

            is AddEditNoteEvent.ClearSuggestions -> {
                _uiState.update { it.copy(suggestedTags = emptyList()) }
            }

            is AddEditNoteEvent.AttachTranscript -> {
                _uiState.update { state ->
                    val currentDoc = state.contentState.richText
                    val newRawText = currentDoc.rawText + "\n" + event.transcript
                    val newDoc = RichTextDocument(rawText = newRawText, spans = currentDoc.spans)
                    val annotated = RichTextAnnotator.toAnnotatedString(newDoc)
                    state.copy(
                        contentState = state.contentState.copy(richText = newDoc),
                        contentTextFieldValue = TextFieldValue(annotatedString = annotated)
                    )
                }
            }

            is AddEditNoteEvent.TranscribeAttachedAudio -> transcribeAttachedAudio(event.uri)
            is AddEditNoteEvent.AnalyzeImage -> analyzeImage(event.uri)
            is AddEditNoteEvent.ExecuteImageQuery -> executeImageQuery(event.query)
            is AddEditNoteEvent.ClearImageSuggestions -> _uiState.update {
                it.copy(imageSuggestions = emptyList(), imageQueryResult = "", isImageQueryLoading = false, isAnalyzingImage = false)
            }
            is AddEditNoteEvent.ClearImageQueryResult -> {
                val cachedKey = _uiState.value.zoomedImageUri?.toString()
                val cached = cachedKey?.let { imageSuggestionsCache[it] } ?: emptyList()
                _uiState.update { it.copy(imageQueryResult = "", isImageQueryLoading = false, imageSuggestions = cached) }
            }
        }
    }

    private fun transcribeAttachedAudio(uriString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(transcribingUri = uriString,
                attachedAudios = it.attachedAudios.map { if (it.uri == uriString) it.copy(transcription = "") else it }
            ) }
            transcribeAudioFileUseCase(uriString) { transcript ->
                _uiState.update { state ->
                    state.copy(
                        attachedAudios = state.attachedAudios.map {
                            if (it.uri == uriString) it.copy(transcription = it.transcription + transcript) else it
                        }
                    )
                }
            }
            _uiState.update { it.copy(transcribingUri = null) }
        }
    }

    private fun readUriBytes(uri: Uri): ByteArray? {
        return readAndProcessImage(appContext, uri, maxDimension = 1024)
    }

    private fun analyzeImage(uri: Uri) {
        val cacheKey = uri.toString()
        val cached = imageSuggestionsCache[cacheKey]
        if (cached != null) {
            if (cachedImageBytes == null) {
                viewModelScope.launch {
                    cachedImageBytes = withContext(Dispatchers.IO) { readUriBytes(uri) }
                }
            }
            _uiState.update { it.copy(imageSuggestions = cached, isAnalyzingImage = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingImage = true, imageSuggestions = emptyList(), imageQueryResult = "") }
            try {
                val imageBytes = withContext(Dispatchers.IO) { readUriBytes(uri) }
                if (imageBytes == null) {
                    _uiState.update { it.copy(isAnalyzingImage = false) }
                    return@launch
                }
                cachedImageBytes = imageBytes
                val suggestions = analyzeImageUseCase(imageBytes).take(3)
                imageSuggestionsCache[cacheKey] = suggestions
                _uiState.update { it.copy(imageSuggestions = suggestions, isAnalyzingImage = false) }
            } catch (e: Exception) {
                Log.e("AddEditNoteViewModel", "Image analysis failed: ${e.message}")
                _uiState.update { it.copy(isAnalyzingImage = false) }
            }
        }
    }

    private fun executeImageQuery(query: String) {
        val imageBytes = cachedImageBytes ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isImageQueryLoading = true, imageSuggestions = emptyList(), imageQueryResult = "") }
            try {
                queryImageUseCase(imageBytes, query).collect { chunk ->
                    _uiState.update { it.copy(imageQueryResult = it.imageQueryResult + chunk) }
                }
                _uiState.update { it.copy(isImageQueryLoading = false) }
            } catch (e: Exception) {
                Log.e("AddEditNoteViewModel", "Image query failed: ${e.message}")
                _uiState.update { it.copy(isImageQueryLoading = false) }
            }
        }
    }

    private fun deleteNote() {
        viewModelScope.launch {
            fileIOUseCases.deleteFiles(_uiState.value.attachedImages.map { it.toString() })
            fileIOUseCases.deleteFiles(_uiState.value.attachedAudios.map { it.uri })
            if(currentNoteId != 0L) { noteUseCases.deleteNote(currentNoteId) }
            _eventFlow.emit(UiEvent.DeleteNote(currentNoteId))
        }
    }

    private fun handleRecording(enableLiveTranscription: Boolean) {
        viewModelScope.launch {
            if (uiState.value.isRecording) {
                _uiState.update { it.copy(isRecording = false) }

                if(enableLiveTranscription) {
                    stopLiveTransctiptions()
                    _uiState.update { state ->
                        val currentDoc = state.contentState.richText
                        val newRawText = currentDoc.rawText + "\n" + state.liveTranscription
                        val newDoc = RichTextDocument(rawText = newRawText, spans = currentDoc.spans)
                        val annotated = RichTextAnnotator.toAnnotatedString(newDoc)
                        state.copy(
                            contentState = state.contentState.copy(richText = newDoc),
                            contentTextFieldValue = TextFieldValue(annotatedString = annotated),
                            liveTranscription = ""
                        )
                    }
                } else {
                    audioRecorder.stopRecording()
                    currentRecordingFile?.let { file ->
                        val uri = Uri.fromFile(file)
                        val duration = audioMetadataProvider.getDuration(uri)
                        _uiState.update { it.copy(
                            attachedAudios = it.attachedAudios + Attachment(uri.toString(), duration)
                        )}
                    }
                }
            } else {
                if(enableLiveTranscription) {
                    _uiState.update { state -> state.copy(isRecording = true) }
                    startLiveTransctiption(onTranscription = { transcript ->
                        Log.d("kptest", "Transcription: $transcript")
                        _uiState.update { it.copy(liveTranscription = it.liveTranscription + " " + transcript) }
                    })
                }else {
                    val file = fileIOUseCases.createFile(AttachmentType.AUDIO.extension, AttachmentType.AUDIO.name.lowercase())
                    currentRecordingFile = file
                    file?.let {
                        audioRecorder.startRecording(it)
                        _uiState.update { state -> state.copy(isRecording = true) }
                    }
                }
            }
        }
    }

    private suspend fun saveNote(id:Long): Result<Long> {
        val state = uiState.value
        return noteUseCases.saveNoteWithAttachments.invoke(
            id = id,
            title = state.titleState.richText.rawText,
            content = RichTextSerializer.serialize(state.contentState.richText),
            timestamp = System.currentTimeMillis(),
            color = state.noteColor,
            imageUris = state.attachedImages,
            audioUris = state.attachedAudios.map { it.uri.toUri() },
            audioTranscriptions = state.attachedAudios.map { it.transcription },
            reminderTime = state.reminderTime,
            checkboxItems = state.checkListItems,
            tagEntities = state.tagEntities
        )
    }

    private fun rescheduleReminder(id: Long){
        val state = uiState.value
        reminderScheduler.cancel(id = id)
        if(state.reminderTime > System.currentTimeMillis()) {
            reminderScheduler.schedule(
                id = id,
                title = state.titleState.richText.rawText,
                content = state.contentState.richText.rawText,
                reminderTime = state.reminderTime
            )
        }
    }

    fun generateTempUri(attachmentType: AttachmentType): Uri? {
        return fileIOUseCases.createUri(attachmentType.extension, attachmentType.name.lowercase())
    }

    fun checkMicrophonePermission(isGranted: Boolean, shouldShowRationale: Boolean) {
        viewModelScope.launch {
            val hasAskedBefore = permissionUsecases.getMicrophonePermissionFlag()
            when {
                isGranted -> _eventFlow.emit(UiEvent.LaunchAudioRecorder)
                shouldShowRationale -> _uiState.update { it.copy(showMicrophoneRationale = true) }
                hasAskedBefore -> _uiState.update { it.copy(settingsDeniedType = DeniedType.MICROPHONE) }
                else -> permissionUsecases.markMicrophonePermissionFlag()
            }
        }
    }

    fun checkCameraPermission(isGranted: Boolean, shouldShowRationale: Boolean, attachmentType: AttachmentType) {
        viewModelScope.launch {
            val hasAskedBefore = permissionUsecases.getCameraPermissionFlag()
            when {
                isGranted -> _eventFlow.emit(UiEvent.LaunchCamera(attachmentType))
                shouldShowRationale -> _uiState.update { it.copy(showCameraRationale = true) }
                hasAskedBefore -> _uiState.update { it.copy(settingsDeniedType = DeniedType.CAMERA) }
                else -> permissionUsecases.markCameraPermissionFlag()
            }
        }
    }

    fun checkExactAlarmPermission() {
        viewModelScope.launch {
            if(!reminderScheduler.canScheduleExactAlarms()){
                _uiState.update { it.copy(showAlarmPermissionRationale = true) }
            } else{
                _uiState.update { it.copy(
                    showAlarmPermissionRationale = false,
                    showDataAndTimePicker = true
                ) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer.stop()
        if (uiState.value.isRecording) {
            viewModelScope.launch {
                stopLiveTransctiptions()
                audioRecorder.stopRecording()
            }
        }
    }

    private data class DerivedPending(
        val block: SpanType?,
        val bullet: Boolean,
        val inline: Set<SpanType>
    )

    /**
     * Derives pending block/bullet/inline from the cursor position.
     * Block types and BULLET are derived from the **current line's spans**
     * (not char-left-of-cursor) so that moving to an empty line after a
     * bullet line doesn't inherit the previous line's bullet state.
     * Inline styles use char-left-of-cursor for natural continuation.
     */
    private fun derivePendingFromCursor(doc: RichTextDocument, cursor: Int): DerivedPending {
        // Inline styles: char left of cursor (natural typing continuation)
        val inlineLookup = (cursor - 1).coerceAtLeast(0)
        val inlineTypes = doc.activeTypesAt(inlineLookup)
            .filter { it !in SpanAdjustmentEngine.BLOCK_TYPES && it != SpanType.BULLET }
            .toSet()

        // Block/bullet: check spans covering the current line
        val lineRanges = SpanAdjustmentEngine.getLineRanges(doc.rawText, cursor, cursor)
        val lineStart = lineRanges.firstOrNull()?.first ?: 0
        val lineEnd = lineRanges.lastOrNull()?.second ?: 0

        val block: SpanType?
        val bullet: Boolean
        if (lineEnd > lineStart) {
            val lineTypes = doc.activeTypesAt(lineStart)
            block = lineTypes.firstOrNull { it in SpanAdjustmentEngine.BLOCK_TYPES }
            bullet = SpanType.BULLET in lineTypes
        } else {
            // Empty line — no spans to read, return null/false
            block = null
            bullet = false
        }

        return DerivedPending(block, bullet, inlineTypes)
    }

    private fun buildActiveStyles(
        block: SpanType?,
        bullet: Boolean,
        inline: Set<SpanType>
    ): Set<SpanType> = setOfNotNull(block) +
            (if (bullet) setOf(SpanType.BULLET) else emptySet()) +
            inline

    sealed class UiEvent{
        data class ShowSnackbar(val message:String):UiEvent()
        data object SaveNote: UiEvent()
        data class DeleteNote(val id: Long): UiEvent()
        data class MakeCopy(val newNoteId: Long): UiEvent()
        data object LaunchAudioRecorder : UiEvent()
        data class LaunchCamera(val type: AttachmentType) : UiEvent()
        data class RequestWidgetPin(val noteId: Long):UiEvent()
    }
}
