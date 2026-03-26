package com.mintanable.notethepad.feature_note.presentation.modify

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.mintanable.notethepad.core.common.CheckboxConvertors
import com.mintanable.notethepad.database.db.entity.Attachment
import com.mintanable.notethepad.database.db.entity.AttachmentType
import com.mintanable.notethepad.core.model.note.CheckboxItem
import com.mintanable.notethepad.NoteColors
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
import kotlinx.coroutines.launch
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
    private val tagUseCases: TagUseCases,
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
                _uiState.update { it.copy(titleState = it.titleState.copy(text = passedInitialTitle, isHintVisible = false)) }
            }
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            noteUseCases.getDetailedNote(id)?.also { detailedNote ->
                currentNoteId = detailedNote.id

                _uiState.update {
                    it.copy(
                        titleState = it.titleState.copy(text = detailedNote.title, isHintVisible = false),
                        contentState = it.contentState.copy(text = detailedNote.content, isHintVisible = false),
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
                    titleState = it.titleState.copy(text = event.value)
                )}
            }
            is AddEditNoteEvent.ChangeTitleFocus -> {
                _uiState.update { it.copy(
                    titleState = it.titleState.copy(
                        isHintVisible = !event.focusState.isFocused && it.titleState.text.isBlank()
                    )
                )}
            }
            is AddEditNoteEvent.EnteredContent -> {
                _uiState.update { it.copy(
                    contentState = it.contentState.copy(text = event.value)
                )}
            }
            is AddEditNoteEvent.ChangeContentFocus -> {
                _uiState.update { it.copy(
                    contentState = it.contentState.copy(
                        isHintVisible = !event.focusState.isFocused && it.contentState.text.isBlank()
                    )
                )}
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
                mediaPlayer.playPause(event.uri)
                _uiState.update { it.copy(zoomedImageUri = event.uri) }

                val attachmentType = AttachmentHelper.getAttachmentType(appContext, event.uri)
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
                    _uiState.update {
                        it.copy(
                            isCheckboxListAvailable = false,
                            checkListItems = emptyList(),
                            contentState = it.contentState.copy(
                                text = CheckboxConvertors.checkboxesToContentString(
                                    uiState.value.checkListItems
                                ), isHintVisible = false
                            )
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isCheckboxListAvailable = true,
                            checkListItems = CheckboxConvertors.stringToCheckboxes(it.contentState.text),
                            contentState = it.contentState.copy(
                                text = "", isHintVisible = false
                            )
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
                    getAutoTagsUseCase(title = uiState.value.titleState.text, content = uiState.value.contentState.text)
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
                _uiState.update { it.copy(contentState = it.contentState.copy(text = it.contentState.text + "\n" + event.transcript)) }
            }

            is AddEditNoteEvent.TranscribeAttachedAudio -> transcribeAttachedAudio(event.uri)
            is AddEditNoteEvent.AnalyzeImage -> analyzeImage(event.uri)
            is AddEditNoteEvent.ExecuteImageQuery -> executeImageQuery(event.query)
            is AddEditNoteEvent.ClearImageSuggestions -> _uiState.update {
                it.copy(imageSuggestions = emptyList(), imageQueryResult = "", isImageQueryLoading = false, isAnalyzingImage = false)
            }
        }
    }

    private fun transcribeAttachedAudio(uriString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(transcribingUri = uriString) }
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

    private fun analyzeImage(uri: Uri) {
        val cacheKey = uri.toString()
        val cached = imageSuggestionsCache[cacheKey]
        if (cached != null) {
            // Load image bytes for potential query later
            if (cachedImageBytes == null) {
                cachedImageBytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            _uiState.update { it.copy(imageSuggestions = cached, isAnalyzingImage = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingImage = true, imageSuggestions = emptyList(), imageQueryResult = "") }
            try {
                val imageBytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
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
                    _uiState.update { it.copy(contentState = it.contentState.copy(text = it.contentState.text + "\n" + it.liveTranscription)) }
                    _uiState.update { it.copy(liveTranscription = "") }
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
                title = state.titleState.text,
                content = state.contentState.text,
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
                title = state.titleState.text,
                content = state.contentState.text,
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