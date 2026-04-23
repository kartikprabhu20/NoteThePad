package com.mintanable.notethepad.feature_note.presentation.modify

import android.content.Context
import android.content.Intent
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
import androidx.work.*
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.core.common.utils.readAndProcessImage
import com.mintanable.notethepad.core.model.note.CheckboxItem
import com.mintanable.notethepad.core.richtext.compose.RichTextAnnotator
import com.mintanable.notethepad.core.richtext.engine.RichTextEngine
import com.mintanable.notethepad.core.richtext.model.RichTextDocument
import com.mintanable.notethepad.core.richtext.model.RichTextState
import com.mintanable.notethepad.core.richtext.serializer.RichTextSerializer
import com.mintanable.notethepad.database.db.entity.Attachment
import com.mintanable.notethepad.database.db.entity.AttachmentType
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.database.db.util.AudioMetadataProvider
import com.mintanable.notethepad.core.model.ai.AiCapabilities
import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import com.mintanable.notethepad.core.model.settings.User
import com.mintanable.notethepad.feature_ai.domain.toCapabilities
import com.mintanable.notethepad.feature_ai.domain.use_cases.AnalyzeImageUseCase
import com.mintanable.notethepad.feature_ai.domain.use_cases.GetAiModelByName
import com.mintanable.notethepad.feature_ai.domain.use_cases.GetAutoTagsUseCase
import com.mintanable.notethepad.feature_ai.domain.use_cases.QueryImageUseCase
import com.mintanable.notethepad.feature_ai.domain.use_cases.StartLiveTransctiption
import com.mintanable.notethepad.feature_ai.domain.use_cases.StopLiveTranscription
import com.mintanable.notethepad.feature_ai.domain.use_cases.TranscribeAudioFileUseCase
import com.mintanable.notethepad.feature_note.domain.repository.AudioRecorder
import com.mintanable.notethepad.feature_note.domain.repository.MediaPlayer
import com.mintanable.notethepad.core.common.ReminderScheduler
import com.mintanable.notethepad.feature_note.data.pdf.NotePdfExporter
import com.mintanable.notethepad.feature_note.domain.use_case.fileio.FileIOUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.notes.NoteUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.tags.TagUseCases
import com.mintanable.notethepad.feature_note.presentation.AddEditNoteUiState
import com.mintanable.notethepad.core.common.CheckboxConvertors
import com.mintanable.notethepad.file.AttachmentHelper
import com.mintanable.notethepad.permissions.DeniedType
import com.mintanable.notethepad.feature_note.presentation.notes.BottomSheetType
import com.mintanable.notethepad.feature_note.domain.use_case.permissions.PermissionUsecases
import com.mintanable.notethepad.auth.repository.AuthRepository
import com.mintanable.notethepad.database.db.repository.CollaborationRepository
import com.mintanable.notethepad.feature_ai.data.SummarizeNoteWorker
import com.mintanable.notethepad.core.analytics.AnalyticsEvent.*
import com.mintanable.notethepad.core.analytics.AnalyticsTracker
import com.mintanable.notethepad.core.model.collaboration.Collaborator
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.feature_note.presentation.EditNoteSnapshot
import com.mintanable.notethepad.feature_note.presentation.NoteDataState
import com.mintanable.notethepad.feature_note.presentation.modify.UiEvent.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
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
    private val startLiveTranscription: StartLiveTransctiption,
    private val stopLiveTranscription: StopLiveTranscription,
    private val transcribeAudioFileUseCase: TranscribeAudioFileUseCase,
    private val analyzeImageUseCase: AnalyzeImageUseCase,
    private val queryImageUseCase: QueryImageUseCase,
    private val authRepository: AuthRepository,
    private val collaborationRepository: CollaborationRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val getAiModelByName: GetAiModelByName,
    private val analyticsTracker: AnalyticsTracker,
    private val snapshotTracker: SnapshotTracker,
    private val notePdfExporter: NotePdfExporter,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    private var imageQueryJob: Job? = null

    private val passedNoteId: String = savedStateHandle.get<String>("noteId") ?: ""
    private val passedReminderTime: Long = savedStateHandle.get<Long>("reminderTime") ?: -1L
    private val passedInitialTitle: String = savedStateHandle.get<String>("initialTitle") ?: ""
    private val isEditMode = passedNoteId.isNotBlank()
    private var currentNoteId: String = ""
    private var currentUserId: String? = null
    private var currentUser: User? = null
    private var currentOwnerId: String? = null
    private var currentRecordingFile: File? = null
    private val imageSuggestionsCache = mutableMapOf<String, List<String>>()
    private var cachedImageBytes: ByteArray? = null
    private val saveInProgress = AtomicBoolean(false)
    private var baselineNoteData: NoteDataState? = null

    val canUndo = snapshotTracker.canUndo
    val canRedo = snapshotTracker.canRedo

    private val _uiState = MutableStateFlow(
        AddEditNoteUiState(
            noteColor =
                if (isEditMode) -1 else NoteColors.colors.random().toArgb()
        )
    )

    private val snapshotObserver = _uiState
        .map { it.toEditNoteSnapshot() }
        .distinctUntilChanged()
        .debounce(500L)
        .onEach { snapshot ->
            snapshotTracker.applySnapshot(snapshot)
        }
        .launchIn(viewModelScope)

    private fun observeCurrentUser() {
        viewModelScope.launch {
            authRepository.getSignedInFirebaseUser().collect { user ->
                currentUserId = user?.uid
                currentUser = user
                updateOwnerStatus()
            }
        }
    }

    private fun updateOwnerStatus() {
        val isOwner =
            currentUserId != null && (currentOwnerId == null || currentOwnerId == currentUserId)
        _uiState.update { it.copy(isOwner = isOwner) }
    }

    private fun observeCollaborators(noteId: String) {
        viewModelScope.launch {
            collaborationRepository.getCollaboratorsForNote(noteId).collect { entities ->
                if (entities.isNotEmpty()) {
                    currentOwnerId = entities.first().ownerUserId
                    updateOwnerStatus()
                }
                val mapped = entities.map { entity ->
                    Collaborator(
                        id = entity.id,
                        noteId = entity.noteId,
                        userId = entity.collaboratorUserId,
                        email = entity.collaboratorEmail,
                        displayName = entity.collaboratorDisplayName,
                        photoUrl = entity.collaboratorPhotoUrl,
                        isOwner = entity.collaboratorUserId == entity.ownerUserId,
                        isCurrentUser = entity.collaboratorUserId == currentUserId
                    )
                }
                val hasOwner = mapped.any { it.isOwner }
                val collaborators = if (!hasOwner && mapped.isNotEmpty() && currentUserId == currentOwnerId) {
                    val user = currentUser
                    if (user != null) {
                        val ownerEntry = Collaborator(
                            id = "owner_${user.uid}",
                            noteId = noteId,
                            userId = user.uid,
                            email = user.email ?: "",
                            displayName = user.displayName,
                            photoUrl = user.photoUrl,
                            isOwner = true
                        )
                        listOf(ownerEntry) + mapped
                    } else mapped
                } else {
                    mapped
                }
                _uiState.update { state ->
                    state.copy(
                        collaborators = collaborators,
                        isLoadingCollaborators = false
                    )
                }
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCollaborators = true) }
            try {
                collaborationRepository.fetchAndCacheCollaborators(noteId)
            } catch (e: Exception) {
                Log.w("AddEditNoteViewModel", "Failed to fetch collaborators: ${e.message}")
            }
            _uiState.update { it.copy(isLoadingCollaborators = false) }
        }
    }

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

    val aiCapabilities: StateFlow<AiCapabilities> = userPreferencesRepository
        .settingsFlow
        .map { it.aiModelName }
        .distinctUntilChanged()
        .map { name -> getAiModelByName(name).toCapabilities() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AiCapabilities.NONE
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
        observeCurrentUser()
        loadNote(passedNoteId)
        if (!isEditMode) {
            if (passedReminderTime > 0L) {
                _uiState.update { it.copy(reminderTime = passedReminderTime) }
            }
            if (passedInitialTitle.isNotBlank()) {
                _uiState.update {
                    it.copy(
                        titleState = it.titleState.copy(
                            richText = RichTextDocument(rawText = passedInitialTitle),
                            isHintVisible = false
                        )
                    )
                }
            }
        }
        baselineNoteData = _uiState.value.toNoteDataState()
    }

    private fun loadNote(id: String) {
        if (id.isBlank()) return
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
                        contentRichTextState = it.contentRichTextState.copy(
                            document = richText,
                            textFieldValue = TextFieldValue(annotatedString = annotated)
                        ),
                        noteColor = detailedNote.color,
                        backgroundImage = detailedNote.backgroundImage,
                        attachedImages = detailedNote.imageUris.map { imgString -> imgString.toUri() },
                        attachedPaints = detailedNote.paintUris,
                        attachedAudios = detailedNote.audioAttachments,
                        reminderTime = detailedNote.reminderTime,
                        checkListItems = detailedNote.checkListItems,
                        isCheckboxListAvailable = detailedNote.isCheckboxListAvailable,
                        tagEntities = detailedNote.tagEntities,
                        summary = detailedNote.summary
                    )
                }
                baselineNoteData = _uiState.value.toNoteDataState()
                observeCollaborators(id)
            }
        }
    }

    fun onEvent(event: AddEditNoteEvent) {
        Log.d("AddEditNoteViewModel", "event: $event")
        when (event) {
            is AddEditNoteEvent.EnteredTitle -> {
                _uiState.update {
                    it.copy(
                        titleState = it.titleState.copy(
                            richText = RichTextDocument(rawText = event.value),
                            isHintVisible = event.value.isBlank() && !it.titleState.isFocused,
                        )
                    )
                }
            }

            is AddEditNoteEvent.ChangeTitleFocus -> {
                _uiState.update {
                    it.copy(
                        titleState = it.titleState.copy(
                            isFocused = event.focusState.isFocused,
                            isHintVisible = !event.focusState.isFocused && it.titleState.richText.rawText.isBlank()
                        )
                    )
                }
            }

            is AddEditNoteEvent.EnteredContent -> {
                _uiState.update { state ->
                    val newState =
                        RichTextEngine.onValueChanged(state.contentRichTextState, event.value)
                    state.copy(
                        contentRichTextState = newState,
                        contentState = state.contentState.copy(
                            isHintVisible = newState.document.rawText.isBlank() && !state.contentState.isFocused
                        )
                    )
                }
            }

            is AddEditNoteEvent.ChangeContentFocus -> {
                val focused = event.focusState.isFocused
                _uiState.update {
                    it.copy(
                        contentState = it.contentState.copy(
                            isFocused = focused,
                            isHintVisible = !focused && it.contentRichTextState.document.rawText.isBlank()
                        ),
                        isRichTextBarActive = if (!focused) false else it.isRichTextBarActive,
                        contentRichTextState = if (!focused) it.contentRichTextState.copy(
                            pendingBlockType = null,
                            pendingBullet = false,
                            pendingStyles = emptySet()
                        ) else it.contentRichTextState
                    )
                }
            }

            is AddEditNoteEvent.ApplyContentFormat -> {
                analyticsTracker.track(RichTextFormatApplied(event.type.name.lowercase()))
                _uiState.update { state ->
                    val newState =
                        RichTextEngine.toggleFormat(state.contentRichTextState, event.type)
                    state.copy(contentRichTextState = newState)
                }
            }

            is AddEditNoteEvent.ToggleRichTextBar -> {
                val newShown = !_uiState.value.isRichTextBarActive
                analyticsTracker.track(RichTextBarToggled(newShown))
                _uiState.update { it.copy(isRichTextBarActive = newShown) }
            }

            is AddEditNoteEvent.ChangeColor -> {
                _uiState.update { it.copy(noteColor = event.color, backgroundImage = -1) }
            }

            is AddEditNoteEvent.ChangeBackgroundImage -> {
                _uiState.update { it.copy(backgroundImage = event.index, noteColor = -1) }
            }

            is AddEditNoteEvent.SaveNote -> {
                if (!saveInProgress.compareAndSet(false, true)) return
                viewModelScope.launch {
                    try {
                        val state = _uiState.value
                        val rawTitle = state.titleState.richText.rawText
                        val contentText = state.contentRichTextState.document.rawText
                      
                        val isNew = currentNoteId.isBlank()
                        val currentData = state.toNoteDataState()
                        if (currentData == baselineNoteData) {
                            _eventFlow.emit(UiEvent.SaveNote)
                            return@launch
                        }

                        val effectiveTitle = rawTitle.ifBlank {
                            contentText.lineSequence()
                                .map { it.trim() }
                                .firstOrNull { it.isNotEmpty() }
                                .orEmpty()
                                .take(60)
                                .ifBlank { appContext.getString(R.string.untitled_note_fallback) }
                        }

                        if (effectiveTitle != rawTitle) {
                            _uiState.update {
                                it.copy(
                                    titleState = it.titleState.copy(
                                        richText = RichTextDocument(rawText = effectiveTitle),
                                        isHintVisible = false
                                    )
                                )
                            }
                        }

                        _uiState.update { it.copy(isSaving = true) }
                        saveNote(currentNoteId, titleOverride = effectiveTitle)
                            .onSuccess { id ->
                                if (currentNoteId.isBlank() && id.isNotBlank()) {
                                    currentNoteId = id
                                }
                                if (id.isNotBlank()) rescheduleReminder(id)
                                val s = _uiState.value
                                val attCount = s.attachedImages.size + s.attachedAudios.size
                                val evt = if (isNew) NoteCreated(
                                    hasAttachments = attCount > 0,
                                    attachmentCount = attCount,
                                    tagCount = s.tagEntities.size,
                                    contentLen = s.contentRichTextState.document.rawText.length,
                                    isChecklist = s.isCheckboxListAvailable
                                ) else NoteUpdated(
                                    hasAttachments = attCount > 0,
                                    attachmentCount = attCount,
                                    tagCount = s.tagEntities.size,
                                    contentLen = s.contentRichTextState.document.rawText.length,
                                    isChecklist = s.isCheckboxListAvailable
                                )
                                analyticsTracker.track(evt)
                                baselineNoteData = _uiState.value.toNoteDataState()
                                _uiState.update { it.copy(isSaving = false, zoomedImageUri = null) }
                                _eventFlow.emit(SaveNote)
                            }
                            .onFailure { e ->
                                Log.e("AddEditNoteViewModel", "Save failed", e)
                                _uiState.update { it.copy(isSaving = false) }
                                _eventFlow.emit(
                                    ShowSnackbar(
                                        e.message ?: appContext.getString(R.string.msg_save_failed)
                                    )
                                )
                            }
                    } finally {
                        saveInProgress.set(false)
                        snapshotTracker.clear()
                    }
                }
            }

            is AddEditNoteEvent.MakeCopy -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isSaving = true) }
                    saveNote("")// as new note
                        .onSuccess { id ->
                            _uiState.update { it.copy(isSaving = false, zoomedImageUri = null) }
                            _eventFlow.emit(MakeCopy(id))
                        }
                        .onFailure { e ->
                            _uiState.update { it.copy(isSaving = false) }
                            _eventFlow.emit(
                                ShowSnackbar(
                                    e.message ?: appContext.getString(R.string.msg_save_failed)
                                )
                            )
                        }
                }
            }

            is AddEditNoteEvent.DeleteNote -> {
                analyticsTracker.track(NoteDeleted())
                deleteNote()
                _uiState.update { it.copy(showDeleteConfirmation = false) }
            }

            is AddEditNoteEvent.ToggleDeleteConfirmation -> {
                _uiState.update { it.copy(showDeleteConfirmation = event.show) }
            }

            is AddEditNoteEvent.PinNote -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isSaving = true) }
                    saveNote(currentNoteId)
                        .onSuccess { id ->
                            _uiState.update { it.copy(isSaving = false) }
                            _eventFlow.emit(RequestWidgetPin(id))
                        }
                        .onFailure {
                            _uiState.update { it.copy(isSaving = false) }
                            _eventFlow.emit(ShowSnackbar(appContext.getString(R.string.msg_pin_failed)))
                        }
                }
            }

            is AddEditNoteEvent.AttachImage -> {
                if (!_uiState.value.attachedImages.contains(event.uri)) {
                    val attachmentType = AttachmentHelper.getAttachmentType(appContext, event.uri)
                    analyticsTracker.track(
                        AttachmentAdded(
                            if (attachmentType == AttachmentType.IMAGE) "image" else "video",
                            event.source
                        )
                    )
                }
                _uiState.update {
                    it.copy(
                        attachedImages = if (it.attachedImages.contains(event.uri)) it.attachedImages else it.attachedImages + event.uri
                    )
                }
            }

            is AddEditNoteEvent.RemoveImage -> {
                val attachmentType = AttachmentHelper.getAttachmentType(appContext, event.uri)
                analyticsTracker.track(AttachmentRemoved(if (attachmentType == AttachmentType.IMAGE) "image" else "video"))
                _uiState.update { it.copy(attachedImages = it.attachedImages - event.uri) }
                viewModelScope.launch { fileIOUseCases.deleteFiles(listOf(event.uri.toString())) }
            }

            is AddEditNoteEvent.AttachPaint -> {
                _uiState.update { state ->
                    if (state.attachedPaints.contains(event.path)) state
                    else state.copy(attachedPaints = state.attachedPaints + event.path)
                }
            }

            is AddEditNoteEvent.RemovePaint -> {
                analyticsTracker.track(AttachmentRemoved("paint"))
                _uiState.update { it.copy(attachedPaints = it.attachedPaints - event.path) }
                viewModelScope.launch { fileIOUseCases.deleteFiles(listOf(event.path)) }
            }

            is AddEditNoteEvent.RemoveAudio -> {
                analyticsTracker.track(AttachmentRemoved("audio"))
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
                _uiState.update {
                    it.copy(
                        showCameraRationale = false,
                        showMicrophoneRationale = false,
                        settingsDeniedType = null,
                        showAddNewTagDialog = false
                    )
                }
            }

            is AddEditNoteEvent.UpdateSheetType -> {
                _uiState.update { it.copy(currentSheetType = event.sheetType) }
            }

            is AddEditNoteEvent.ToggleZoom -> {
                val attachmentType = AttachmentHelper.getAttachmentType(appContext, event.uri)
                if (attachmentType == AttachmentType.VIDEO) {
                    mediaPlayer.playPause(event.uri)
                }
                val images = _uiState.value.attachedImages
                val index = images.indexOf(event.uri).coerceAtLeast(0)
                val cacheKey = event.uri.toString()
                val cached = imageSuggestionsCache[cacheKey]
                if (cached != null && cachedImageBytes == null) {
                    viewModelScope.launch {
                        cachedImageBytes = withContext(Dispatchers.IO) { readUriBytes(event.uri) }
                    }
                }
                _uiState.update {
                    it.copy(
                        zoomedImageUri = event.uri,
                        zoomedImageIndex = index,
                        imageSuggestions = cached ?: emptyList(),
                        isAnalyzingImage = false,
                        imageQueryResult = "",
                        isImageQueryLoading = false
                    )
                }
            }

            is AddEditNoteEvent.UpdateNowPlaying -> {
                mediaPlayer.playPause(event.uri.toUri())
            }

            is AddEditNoteEvent.StopMedia -> {
                if (_uiState.value.isImageQueryLoading) {
                    _uiState.update { it.copy(showStopAIConfirmation = true) }
                    return
                }
                mediaPlayer.stop()
                _uiState.update {
                    it.copy(
                        zoomedImageUri = null,
                        zoomedImageIndex = 0,
                        imageSuggestions = emptyList(),
                        imageQueryResult = "",
                        isImageQueryLoading = false,
                        isAnalyzingImage = false,
                        showStopAIConfirmation = false
                    )
                }
            }

            is AddEditNoteEvent.ConfirmStopImageQuery -> {
                if (event.shouldStop) {
                    imageQueryJob?.cancel()
                    _uiState.update {
                        it.copy(
                            isImageQueryLoading = false,
                            showStopAIConfirmation = false,
                            zoomedImageUri = null,
                            zoomedImageIndex = 0,
                            imageSuggestions = emptyList(),
                            imageQueryResult = "",
                            isAnalyzingImage = false
                        )
                    }
                    mediaPlayer.stop()
                } else {
                    _uiState.update { it.copy(showStopAIConfirmation = false) }
                }
            }

            is AddEditNoteEvent.SetReminder -> {
                _uiState.update {
                    it.copy(
                        reminderTime = event.timestamp,
                        showAlarmPermissionRationale = false,
                        showDataAndTimePicker = false
                    )
                }
            }

            is AddEditNoteEvent.CancelReminder -> {
                _uiState.update {
                    it.copy(
                        reminderTime = -1,
                        showAlarmPermissionRationale = false,
                        showDataAndTimePicker = false
                    )
                }
                if (currentNoteId.isNotBlank()) {
                    reminderScheduler.cancel(currentNoteId.hashCode().toLong())
                }
            }

            is AddEditNoteEvent.CheckAlarmPermission -> {
                checkExactAlarmPermission()
            }

            is AddEditNoteEvent.DismissReminder -> {
                _uiState.update {
                    it.copy(
                        showAlarmPermissionRationale = false,
                        showDataAndTimePicker = false
                    )
                }
            }

            is AddEditNoteEvent.ToggleCheckbox -> {
                if (uiState.value.isCheckboxListAvailable) {
                    val textContent =
                        CheckboxConvertors.checkboxesToContentString(uiState.value.checkListItems)
                    val richText = RichTextDocument(rawText = textContent)
                    val annotated = RichTextAnnotator.toAnnotatedString(richText)
                    _uiState.update {
                        it.copy(
                            isCheckboxListAvailable = false,
                            checkListItems = emptyList(),
                            contentState = it.contentState.copy(isHintVisible = false),
                            contentRichTextState = it.contentRichTextState.copy(
                                document = richText,
                                textFieldValue = TextFieldValue(annotatedString = annotated)
                            )
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isCheckboxListAvailable = true,
                            checkListItems = CheckboxConvertors.stringToCheckboxes(it.contentRichTextState.document.rawText),
                            contentState = it.contentState.copy(isHintVisible = false),
                            contentRichTextState = RichTextState.EMPTY
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
                    val targetIndex =
                        currentItems.indexOfFirst { it.id == event.previousCheckItem.id }

                    val newList = if (targetIndex != -1) {
                        currentItems.toMutableList().apply {
                            add(
                                targetIndex + 1,
                                CheckboxItem(
                                    text = "",
                                    isChecked = event.previousCheckItem.isChecked
                                )
                            )
                        }
                    } else {
                        currentItems + CheckboxItem(
                            text = "",
                            isChecked = event.previousCheckItem.isChecked
                        )
                    }

                    currentState.copy(checkListItems = newList)
                }
            }

            is AddEditNoteEvent.ShowLabelDialog -> {
                _uiState.update { it.copy(showAddNewTagDialog = true) }
            }

            is AddEditNoteEvent.InsertLabel -> {
                _uiState.update { currentState ->
                    val newTagEntities =
                        if (currentState.tagEntities.map { it.tagName }.contains(event.tagName)) {
                            currentState.tagEntities
                        } else {
                            currentState.tagEntities + TagEntity(event.tagName)
                        }
                    val newSuggestedTags = if (currentState.suggestedTags.contains(event.tagName)) {
                        currentState.suggestedTags - event.tagName
                    } else {
                        currentState.suggestedTags
                    }
                    currentState.copy(
                        tagEntities = newTagEntities,
                        showAddNewTagDialog = false,
                        suggestedTags = newSuggestedTags
                    )
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
                    val contentLen = uiState.value.contentRichTextState.document.rawText.length
                    val modelName = userPreferencesRepository.settingsFlow.first().aiModelName
                    analyticsTracker.track(AiAutoTagTriggered(modelName, contentLen))
                    _uiState.update { it.copy(isTagSuggestionLoading = true) }
                    getAutoTagsUseCase(
                        title = uiState.value.titleState.richText.rawText,
                        content = uiState.value.contentRichTextState.document.rawText
                    )
                        .onSuccess { suggestions ->
                            analyticsTracker.track(AiAutoTagResult(true, suggestions.size))
                            _uiState.update {
                                it.copy(
                                    isTagSuggestionLoading = false,
                                    suggestedTags = suggestions
                                )
                            }
                        }
                        .onFailure { exeption ->
                            analyticsTracker.track(
                                AiAutoTagResult(
                                    false,
                                    0,
                                    exeption::class.simpleName
                                )
                            )
                            _uiState.update { it.copy(isTagSuggestionLoading = false) }
                            _eventFlow.emit(
                                ShowSnackbar(
                                    exeption.message
                                        ?: appContext.getString(R.string.msg_save_failed)
                                )
                            )
                        }
                }
            }

            is AddEditNoteEvent.ClearSuggestions -> {
                _uiState.update { it.copy(suggestedTags = emptyList()) }
            }

            is AddEditNoteEvent.AttachTranscript -> {
                _uiState.update { state ->
                    val oldState = state.contentRichTextState
                    val currentText = oldState.document.rawText
                    val appendedText =
                        if (currentText.isEmpty()) event.transcript else "\n" + event.transcript
                    val newText = currentText + appendedText
                    val newTfv = TextFieldValue(
                        text = newText,
                        selection = TextRange(newText.length)
                    )
                    val newRichState = RichTextEngine.onValueChanged(oldState, newTfv)
                    state.copy(
                        contentRichTextState = newRichState,
                        contentState = state.contentState.copy(isHintVisible = false)
                    )
                }
            }

            is AddEditNoteEvent.TranscribeAttachedAudio -> transcribeAttachedAudio(event.uri)
            is AddEditNoteEvent.AnalyzeImage -> analyzeImage(event.uri)
            is AddEditNoteEvent.ExecuteImageQuery -> executeImageQuery(event.query)
            is AddEditNoteEvent.ClearImageSuggestions -> _uiState.update {
                it.copy(
                    imageSuggestions = emptyList(),
                    imageQueryResult = "",
                    isImageQueryLoading = false,
                    isAnalyzingImage = false
                )
            }

            is AddEditNoteEvent.AnalyzeCurrentImage -> {
                val uri = _uiState.value.zoomedImageUri ?: return
                analyzeImage(uri)
            }

            is AddEditNoteEvent.NavigateZoomedImage -> {
                val images = _uiState.value.attachedImages
                if (images.size <= 1) return
                val currentIndex = _uiState.value.zoomedImageIndex
                val newIndex = (currentIndex + event.direction).coerceIn(0, images.lastIndex)
                if (newIndex == currentIndex) return

                imageQueryJob?.cancel()

                val newUri = images[newIndex]
                val cacheKey = newUri.toString()
                val cached = imageSuggestionsCache[cacheKey]

                val attachmentType = AttachmentHelper.getAttachmentType(appContext, newUri)
                if (attachmentType == AttachmentType.VIDEO) {
                    mediaPlayer.playPause(newUri)
                } else {
                    mediaPlayer.stop()
                }

                viewModelScope.launch {
                    cachedImageBytes = withContext(Dispatchers.IO) { readUriBytes(newUri) }
                }

                _uiState.update {
                    it.copy(
                        zoomedImageUri = newUri,
                        zoomedImageIndex = newIndex,
                        imageSuggestions = cached ?: emptyList(),
                        isAnalyzingImage = false,
                        imageQueryResult = "",
                        isImageQueryLoading = false
                    )
                }
            }

            is AddEditNoteEvent.ClearImageQueryResult -> {
                val cachedKey = _uiState.value.zoomedImageUri?.toString()
                val cached = cachedKey?.let { imageSuggestionsCache[it] } ?: emptyList()
                _uiState.update {
                    it.copy(
                        imageQueryResult = "",
                        isImageQueryLoading = false,
                        imageSuggestions = cached
                    )
                }
            }

            is AddEditNoteEvent.ShareNote -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isSaving = true) }
                    saveNote(currentNoteId)
                        .onSuccess { id ->
                            if (currentNoteId.isBlank() && id.isNotBlank()) {
                                currentNoteId = id
                            }
                            if (id.isNotBlank()) {
                                rescheduleReminder(id)
                            }
                            val s = _uiState.value
                            analyticsTracker.track(
                                NoteShared(
                                    hasAttachments = s.attachedImages.isNotEmpty() || s.attachedAudios.isNotEmpty(),
                                    imageCount = s.attachedImages.size,
                                    audioCount = s.attachedAudios.size,
                                    tagCount = s.tagEntities.size
                                )
                            )
                            _uiState.update { it.copy(isSaving = false) }

                            val intent = withContext(Dispatchers.IO) { buildShareIntent() }
                            _eventFlow.emit(ShareNote(intent))
                        }
                        .onFailure { e ->
                            _uiState.update { it.copy(isSaving = false) }
                            _eventFlow.emit(
                                ShowSnackbar(
                                    e.message
                                        ?: appContext.getString(R.string.msg_save_failed_sharing)
                                )
                            )
                        }
                }
            }

            is AddEditNoteEvent.ExportAsPdf -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isSaving = true) }
                    saveNote(currentNoteId)
                        .onSuccess { id ->
                            if (currentNoteId.isBlank() && id.isNotBlank()) {
                                currentNoteId = id
                            }
                            if (id.isNotBlank()) {
                                rescheduleReminder(id)
                            }
                            runCatching { notePdfExporter.export(_uiState.value) }
                                .onSuccess { uri ->
                                    _uiState.update { it.copy(isSaving = false) }
                                    val subject = _uiState.value.titleState.richText.rawText.trim()
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        if (subject.isNotEmpty()) {
                                            putExtra(Intent.EXTRA_SUBJECT, subject)
                                        }
                                        clipData = android.content.ClipData.newRawUri(null, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    _eventFlow.emit(ExportPdf(intent))
                                }
                                .onFailure { e ->
                                    Log.e("AddEditNoteViewModel", "PDF export failed", e)
                                    _uiState.update { it.copy(isSaving = false) }
                                    _eventFlow.emit(
                                        ShowSnackbar(
                                            e.message
                                                ?: appContext.getString(R.string.msg_pdf_export_failed)
                                        )
                                    )
                                }
                        }
                        .onFailure { e ->
                            _uiState.update { it.copy(isSaving = false) }
                            _eventFlow.emit(
                                ShowSnackbar(
                                    e.message ?: appContext.getString(R.string.msg_save_failed)
                                )
                            )
                        }
                }
            }

            is AddEditNoteEvent.InviteCollaborator -> {
                viewModelScope.launch {
                    if (currentNoteId.isBlank()) {
                        _uiState.update { it.copy(isSaving = true) }
                        saveNote(currentNoteId)
                            .onSuccess { id ->
                                if (id.isNotBlank()) {
                                    rescheduleReminder(id)
                                }
                                _uiState.update { it.copy(isSaving = false) }
                            }
                            .onFailure { e ->
                                _uiState.update { it.copy(isSaving = false) }
                                _eventFlow.emit(
                                    ShowSnackbar(
                                        e.message ?: appContext.getString(R.string.msg_save_failed)
                                    )
                                )
                                return@launch
                            }
                    }
                    _uiState.update {
                        it.copy(
                            isLoadingCollaborators = true,
                            collaboratorError = null
                        )
                    }
                    val collaborator = collaborationRepository.findUserByEmail(event.email)
                    if (collaborator == null) {
                        _uiState.update {
                            it.copy(
                                isLoadingCollaborators = false,
                                collaboratorError = appContext.getString(R.string.collaborator_not_found)
                            )
                        }
                        _eventFlow.emit(ShowSnackbar(appContext.getString(R.string.collaborator_not_found)))
                    } else if (collaborator.userId == currentUserId) {
                        _uiState.update {
                            it.copy(
                                isLoadingCollaborators = false,
                                collaboratorError = appContext.getString(R.string.cannot_invite_self)
                            )
                        }
                        _eventFlow.emit(ShowSnackbar(appContext.getString(R.string.cannot_invite_self)))
                    } else if (_uiState.value.collaborators.any { it.userId == collaborator.userId }) {
                        _uiState.update {
                            it.copy(
                                isLoadingCollaborators = false,
                                collaboratorError = appContext.getString(R.string.collaborator_already_added)
                            )
                        }
                        _eventFlow.emit(ShowSnackbar(appContext.getString(R.string.collaborator_already_added)))
                    } else {
                        val success = collaborationRepository.addCollaborator(
                            noteId = currentNoteId,
                            ownerUserId = currentUserId ?: "",
                            collaborator = collaborator
                        )
                        analyticsTracker.track(CollaboratorInvited(success))
                        if (success) {
                            _uiState.update { it.copy(isLoadingCollaborators = false) }
                            _eventFlow.emit(
                                ShowSnackbar(
                                    appContext.getString(
                                        R.string.msg_invitation_sent,
                                        event.email
                                    )
                                )
                            )
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoadingCollaborators = false,
                                    collaboratorError = appContext.getString(R.string.msg_remove_collaborator_failed)
                                )
                            }
                            _eventFlow.emit(ShowSnackbar(appContext.getString(R.string.msg_remove_collaborator_failed)))
                        }
                    }
                }
            }

            AddEditNoteEvent.LeaveNote -> {
                viewModelScope.launch {
                    val userId = currentUserId ?: return@launch
                    val success = collaborationRepository.leaveNote(currentNoteId, userId)
                    if (success) {
                        _eventFlow.emit(SaveNote) // Use SaveNote to trigger navigation up
                    } else {
                        _eventFlow.emit(ShowSnackbar(appContext.getString(R.string.msg_leave_failed)))
                    }
                }
            }

            AddEditNoteEvent.OpenCollaborateSheet -> {
                if(_uiState.value.showLoginAndEnableCloudSyncDialog){
                    _uiState.update { it.copy(showLoginAndEnableCloudSyncDialog = false) }
                    return
                }

                viewModelScope.launch {
                    _uiState.update { it.copy(isSaving = true) }
                    saveNote(currentNoteId)
                        .onSuccess { id ->
                            if(currentUser != null && userPreferencesRepository.settingsFlow.first().supaSyncEnabled) {
                                _uiState.update { it.copy(isSaving = false, currentSheetType = BottomSheetType.COLLABORATORS) }
                            } else {
                                _uiState.update { it.copy(isSaving = false, currentSheetType = BottomSheetType.NONE, showLoginAndEnableCloudSyncDialog = true) }
                            }
                        }
                        .onFailure { e ->
                            _uiState.update { it.copy(isSaving = false, currentSheetType = BottomSheetType.NONE) }
                            _eventFlow.emit(ShowSnackbar(e.message ?: appContext.getString(R.string.msg_save_failed)))
                        }
                }
            }

            is AddEditNoteEvent.RemoveCollaborator -> {
                analyticsTracker.track(CollaboratorRemoved)
                viewModelScope.launch {
                    val success = collaborationRepository.removeCollaborator(
                        currentNoteId,
                        event.collaboratorUserId
                    )
                    if (!success) {
                        _eventFlow.emit(ShowSnackbar(appContext.getString(R.string.msg_remove_collaborator_failed)))
                    }
                }
            }

            is AddEditNoteEvent.SummarizeNote -> {
                viewModelScope.launch {
                    val s = _uiState.value
                    val modelName = userPreferencesRepository.settingsFlow.first().aiModelName
                    analyticsTracker.track(
                        AiSummarizeTriggered(
                            model = modelName,
                            contentLen = s.contentRichTextState.document.rawText.length,
                            attachmentCount = s.attachedImages.size + s.attachedAudios.size
                        )
                    )
                    // Save note first so worker has latest data
                    val noteId = if (currentNoteId.isNotBlank()) {
                        saveNote(currentNoteId).getOrNull() ?: currentNoteId
                    } else {
                        val result = saveNote(currentNoteId)
                        val savedId = result.getOrNull()
                        if (savedId != null) {
                            currentNoteId = savedId
                            savedId
                        } else {
                            _eventFlow.emit(ShowSnackbar(appContext.getString(R.string.msg_save_before_summarize)))
                            return@launch
                        }
                    }

                    // Enqueue the worker
                    val workRequest = OneTimeWorkRequestBuilder<SummarizeNoteWorker>()
                        .setInputData(workDataOf("note_id" to noteId))
                        .build()
                    val workManager = WorkManager.getInstance(appContext)
                    workManager.enqueue(workRequest)
                    _eventFlow.emit(ShowSnackbar(appContext.getString(R.string.msg_summarization_background)))

                    // Observe completion to update UI
                    workManager.getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                        if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                            noteUseCases.getDetailedNote(noteId)?.let { refreshedNote ->
                                _uiState.update { it.copy(summary = refreshedNote.summary) }
                            }
                            return@collect
                        }
                    }
                }
            }

            is AddEditNoteEvent.EditSummary -> {
                _uiState.update { it.copy(summary = event.text) }
            }

            is AddEditNoteEvent.DeleteSummary -> {
                _uiState.update { it.copy(summary = "") }
            }

            is AddEditNoteEvent.Redo -> {
                handleUndoRedoAction(snapshotTracker.redo())
                analyticsTracker.track(NoteUndoRedo("redo"))
            }
            is AddEditNoteEvent.Undo -> {
                handleUndoRedoAction(snapshotTracker.undo())
                analyticsTracker.track(NoteUndoRedo("undo"))
            }
        }
    }

    private fun handleUndoRedoAction(snapshot: EditNoteSnapshot?) {
        snapshot?.let { snap ->
            _uiState.update { state ->
                val annotatedString = RichTextAnnotator.toAnnotatedString(snap.contentDocument)
                state.copy(
                    titleState = state.titleState.copy(
                        richText = snap.titleDocument,
                        isHintVisible = snap.titleDocument.rawText.isBlank() && !state.titleState.isFocused
                    ),
                    contentState = state.contentState.copy(
                        richText = snap.contentDocument,
                        isHintVisible = snap.contentDocument.rawText.isBlank() && !state.contentState.isFocused
                    ),
                    contentRichTextState = state.contentRichTextState.copy(
                        document = snap.contentDocument,
                        textFieldValue = TextFieldValue(
                            annotatedString = annotatedString,
                            selection = TextRange(annotatedString.length)
                        )
                    ),
                    noteColor = snap.noteColor,
                    backgroundImage = snap.backgroundImage,
                    reminderTime = snap.reminderTime,
                    checkListItems = snap.checkListItems,
                    isCheckboxListAvailable = snap.isCheckboxListAvailable,
                    tagEntities = snap.tagEntities
                )
            }
        }
    }

    private fun buildShareIntent(): Intent {
        val state = uiState.value
        val title = state.titleState.richText.rawText.trim()
        val body = buildString {
            if (state.isCheckboxListAvailable) {
                state.checkListItems.forEach { item ->
                    append(if (item.isChecked) "☑ " else "☐ ")
                    append(item.text)
                    append("\n")
                }
            } else {
                append(state.contentRichTextState.document.rawText.trim())
            }
            if (state.tagEntities.isNotEmpty()) {
                if (isNotEmpty()) append("\n\n")
                append(state.tagEntities.joinToString(" ") { "#${it.tagName}" })
            }
        }

        val imageUris = state.attachedImages.mapNotNull { fileIOUseCases.createContentFromUri(it) }
        val audioUris =
            state.attachedAudios.mapNotNull { fileIOUseCases.createContentFromUri(it.uri.toUri()) }
        val allAttachments = ArrayList(imageUris + audioUris)

        val textBody = if (title.isNotBlank()) "$title\n\n$body" else body

        return if (allAttachments.isEmpty()) {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, textBody)
            }
        } else if (allAttachments.size == 1) {
            val uri = allAttachments[0]
            Intent(Intent.ACTION_SEND).apply {
                type = appContext.contentResolver.getType(uri) ?: "*/*"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, textBody)
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newRawUri(null, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = if (imageUris.isNotEmpty()) "image/*" else "audio/*"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, textBody)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, allAttachments)

                // Essential for granting read permissions to multiple URIs on modern Android
                val clipData = android.content.ClipData.newRawUri(null, allAttachments[0])
                for (i in 1 until allAttachments.size) {
                    clipData.addItem(android.content.ClipData.Item(allAttachments[i]))
                }
                this.clipData = clipData
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    private fun transcribeAttachedAudio(uriString: String) {
        viewModelScope.launch {
            val audioDuration =
                _uiState.value.attachedAudios.find { it.uri == uriString }?.duration ?: 0L
            val modelName = userPreferencesRepository.settingsFlow.first().aiModelName
            analyticsTracker.track(
                AiAudioTranscribeTriggered(
                    audioDuration,
                    modelName
                )
            )
            _uiState.update {
                it.copy(
                    transcribingUri = uriString,
                    attachedAudios = it.attachedAudios.map {
                        if (it.uri == uriString) it.copy(
                            transcription = ""
                        ) else it
                    }
                )
            }
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
            _uiState.update {
                it.copy(
                    isAnalyzingImage = true,
                    imageSuggestions = emptyList(),
                    imageQueryResult = ""
                )
            }
            try {
                val imageBytes = withContext(Dispatchers.IO) { readUriBytes(uri) }
                if (imageBytes == null) {
                    _uiState.update { it.copy(isAnalyzingImage = false) }
                    return@launch
                }
                cachedImageBytes = imageBytes
                val modelName = userPreferencesRepository.settingsFlow.first().aiModelName
                analyticsTracker.track(
                    AiImageAnalyzeTriggered(
                        imageBytes.size,
                        modelName
                    )
                )
                val suggestions = analyzeImageUseCase(imageBytes).take(3)
                imageSuggestionsCache[cacheKey] = suggestions
                analyticsTracker.track(AiImageAnalyzeResult(true, suggestions.size))
                _uiState.update {
                    it.copy(
                        imageSuggestions = suggestions,
                        isAnalyzingImage = false
                    )
                }
            } catch (e: Exception) {
                analyticsTracker.track(AiImageAnalyzeResult(false, 0))
                Log.e("AddEditNoteViewModel", "Image analysis failed: ${e.message}")
                _uiState.update { it.copy(isAnalyzingImage = false) }
            }
        }
    }

    private fun executeImageQuery(query: String) {
        val imageBytes = cachedImageBytes ?: return
        imageQueryJob?.cancel()
        var chunkCount = 0
        imageQueryJob = viewModelScope.launch {
            val modelName = userPreferencesRepository.settingsFlow.first().aiModelName
            analyticsTracker.track(AiImageQuerySubmitted(query.length, modelName))
            _uiState.update {
                it.copy(
                    isImageQueryLoading = true,
                    imageSuggestions = emptyList(),
                    imageQueryResult = ""
                )
            }
            try {
                queryImageUseCase(imageBytes, query).collect { chunk ->
                    chunkCount++
                    _uiState.update { it.copy(imageQueryResult = it.imageQueryResult + chunk) }
                }
                analyticsTracker.track(
                    AiImageQueryResult(
                        true,
                        chunkCount,
                        _uiState.value.imageQueryResult.length
                    )
                )
                _uiState.update { it.copy(isImageQueryLoading = false) }
            } catch (e: Exception) {
                analyticsTracker.track(
                    AiImageQueryResult(
                        false,
                        chunkCount,
                        _uiState.value.imageQueryResult.length
                    )
                )
                Log.e("AddEditNoteViewModel", "Image query failed: ${e.message}")
                _uiState.update { it.copy(isImageQueryLoading = false) }
            }
        }
    }

    private fun deleteNote() {
        viewModelScope.launch {
            fileIOUseCases.deleteFiles(_uiState.value.attachedImages.map { it.toString() })
            fileIOUseCases.deleteFiles(_uiState.value.attachedAudios.map { it.uri })
            fileIOUseCases.deleteFiles(_uiState.value.attachedPaints)
            if (currentNoteId.isNotBlank()) {
                noteUseCases.deleteNote(currentNoteId)
            }
            _eventFlow.emit(DeleteNote(currentNoteId))
        }
    }

    private fun handleRecording(enableLiveTranscription: Boolean) {
        viewModelScope.launch {
            if (uiState.value.isRecording) {
                _uiState.update { it.copy(isRecording = false) }

                if (enableLiveTranscription) {
                    analyticsTracker.track(AiLiveTranscriptionToggled(false))
                    stopLiveTranscription()
                    _uiState.update { state ->
                        val oldState = state.contentRichTextState
                        val currentText = oldState.document.rawText
                        val appendedText =
                            if (currentText.isEmpty()) state.liveTranscription else "\n" + state.liveTranscription
                        val newText = currentText + appendedText
                        val newTfv = TextFieldValue(
                            text = newText,
                            selection = TextRange(newText.length)
                        )
                        val newRichState = RichTextEngine.onValueChanged(oldState, newTfv)
                        state.copy(
                            contentRichTextState = newRichState,
                            contentState = state.contentState.copy(isHintVisible = false),
                            liveTranscription = ""
                        )
                    }
                } else {
                    audioRecorder.stopRecording()
                    currentRecordingFile?.let { file ->
                        val uri = Uri.fromFile(file)
                        val duration = audioMetadataProvider.getDuration(uri)
                        analyticsTracker.track(AttachmentAudioRecorded(duration))
                        _uiState.update {
                            it.copy(
                                attachedAudios = it.attachedAudios + Attachment(
                                    uri.toString(),
                                    duration
                                )
                            )
                        }
                    }
                }
            } else {
                if (enableLiveTranscription) {
                    analyticsTracker.track(AiLiveTranscriptionToggled(true))
                    _uiState.update { state -> state.copy(isRecording = true) }
                    startLiveTranscription(onTranscription = { transcript ->
                        Log.d("kptest", "Transcription: $transcript")
                        _uiState.update { it.copy(liveTranscription = it.liveTranscription + " " + transcript) }
                    })
                } else {
                    val file = fileIOUseCases.createFile(
                        AttachmentType.AUDIO.extension,
                        AttachmentType.AUDIO.name.lowercase()
                    )
                    currentRecordingFile = file
                    file?.let {
                        audioRecorder.startRecording(it)
                        _uiState.update { state -> state.copy(isRecording = true) }
                    }
                }
            }
        }
    }

    private suspend fun saveNote(id: String, titleOverride: String? = null): Result<String> {
        val state = uiState.value
        return noteUseCases.saveNoteWithAttachments.invoke(
            id = id.ifBlank { null },
            title = titleOverride ?: state.titleState.richText.rawText,
            content = RichTextSerializer.serialize(state.contentRichTextState.document),
            timestamp = System.currentTimeMillis(),
            color = state.noteColor,
            imageUris = state.attachedImages,
            audioUris = state.attachedAudios.map { it.uri.toUri() },
            audioTranscriptions = state.attachedAudios.map { it.transcription },
            reminderTime = state.reminderTime,
            checkboxItems = state.checkListItems,
            tagEntities = state.tagEntities,
            backgroundImage = state.backgroundImage,
            summary = state.summary,
            paintUris = state.attachedPaints
        )
    }

    private fun rescheduleReminder(id: String) {
        val state = uiState.value
        val longId = id.hashCode().toLong()
        reminderScheduler.cancel(id = longId)
        if (state.reminderTime > System.currentTimeMillis()) {
            reminderScheduler.schedule(
                id = longId,
                title = state.titleState.richText.rawText,
                content = state.contentRichTextState.document.rawText,
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

    fun checkCameraPermission(
        isGranted: Boolean,
        shouldShowRationale: Boolean,
        attachmentType: AttachmentType
    ) {
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
            if (!reminderScheduler.canScheduleExactAlarms()) {
                _uiState.update { it.copy(showAlarmPermissionRationale = true) }
            } else {
                _uiState.update {
                    it.copy(
                        showAlarmPermissionRationale = false,
                        showDataAndTimePicker = true
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer.stop()
        if (uiState.value.isRecording) {
            viewModelScope.launch {
                stopLiveTranscription()
                audioRecorder.stopRecording()
            }
        }
    }

}