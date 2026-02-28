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
import com.mintanable.notethepad.feature_note.data.repository.AndroidMediaPlayer
import com.mintanable.notethepad.feature_note.data.repository.AudioMetadataProvider
import com.mintanable.notethepad.feature_note.domain.AddEditNoteUiState
import com.mintanable.notethepad.feature_note.domain.model.NoteColors
import com.mintanable.notethepad.feature_note.domain.repository.MediaPlayer
import com.mintanable.notethepad.feature_note.domain.repository.AudioRecorder
import com.mintanable.notethepad.feature_note.domain.repository.ReminderScheduler
import com.mintanable.notethepad.feature_note.domain.use_case.FileIOUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.NoteUseCases
import com.mintanable.notethepad.feature_note.domain.util.AttachmentType
import com.mintanable.notethepad.feature_note.domain.util.Attachment
import com.mintanable.notethepad.feature_settings.presentation.use_cases.PermissionUsecases
import com.mintanable.notethepad.feature_settings.presentation.util.DeniedType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AddEditNoteViewModel @Inject constructor(
    private val noteUseCases: NoteUseCases,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext val context: Context,
    private val permissionUsecases: PermissionUsecases,
    private val audioRecorder: AudioRecorder,
    private val fileIOUseCases: FileIOUseCases,
    private val mediaPlayer: MediaPlayer,
    private val audioMetadataProvider: AudioMetadataProvider,
    private val reminderScheduler: ReminderScheduler
): ViewModel(){

    private val passedNoteId: Long = savedStateHandle.get<Long>("noteId") ?: -1L
    private val isEditMode = passedNoteId != -1L
    private var currentNoteId: Long? = null
    private var currentRecordingFile: File? = null

    private val _uiState = MutableStateFlow(
        AddEditNoteUiState(
            noteColor =
                if (isEditMode) -1 else NoteColors.colors.random().toArgb()
        )
    )
    val uiState: StateFlow<AddEditNoteUiState> = combine(
        _uiState,
        mediaPlayer.mediaState
    ) { state, mediaState ->
//        Log.d("kptest", "state: $state mediaState: $mediaState")
        state.copy(mediaState = mediaState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddEditNoteUiState()
    )

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val videoPlayerEngine: ExoPlayer? = (mediaPlayer as? AndroidMediaPlayer)?.player

    init {
        loadNote(passedNoteId)
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            noteUseCases.getNote(id)?.also { note ->
                currentNoteId = note.id
                val attachments = note.audioUris.map { uriString ->
                    val uri = uriString.toUri()
                    Attachment(uri, audioMetadataProvider.getDuration(uri))
                }

                _uiState.update {
                    it.copy(
                        titleState = it.titleState.copy(text = note.title, isHintVisible = false),
                        contentState = it.contentState.copy(text = note.content, isHintVisible = false),
                        noteColor = note.color,
                        attachedImages = note.imageUris.map { it.toUri() },
                        attachedAudios = attachments,
                        reminderTime = note.reminderTime
                    )
                }
            }
        }
    }

    fun onEvent(event:AddEditNoteEvent){
//        Log.d("kptest", "event: $event")
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
                saveNote(currentNoteId)
            }
            is AddEditNoteEvent.MakeCopy -> {
                saveNote(null)
            }
            is AddEditNoteEvent.DeleteNote -> {
                deleteNote()
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
                _uiState.update { state ->
                    state.copy(
                        attachedAudios = state.attachedAudios.filterNot { it.uri == event.uri }
                    )
                }
                // If the removed audio was playing, stop the player
                if (uiState.value.mediaState?.currentUri == event.uri.toString()) {
                    mediaPlayer.stop()
                }
            }
            is AddEditNoteEvent.ToggleAudioRecording -> {
                handleRecording()
            }
            is AddEditNoteEvent.DismissDialogs -> {
                _uiState.update { it.copy(
                    showCameraRationale = false,
                    showMicrophoneRationale = false,
                    settingsDeniedType = null
                )}
            }
            is AddEditNoteEvent.UpdateSheetType -> {
                _uiState.update { it.copy(currentSheetType = event.sheetType) }
            }
            is AddEditNoteEvent.ToggleZoom -> {
                mediaPlayer.playPause(event.uri)
                _uiState.update { it.copy(zoomedImageUri = event.uri) }
            }
            is AddEditNoteEvent.UpdateNowPlaying -> {
                mediaPlayer.playPause(event.uri)
            }
            is AddEditNoteEvent.StopMedia -> {
                mediaPlayer.stop()
                _uiState.update { it.copy(zoomedImageUri = null) }
            }
            is AddEditNoteEvent.SetReminder -> {
                _uiState.update { it.copy(reminderTime = event.timestamp, showAlarmPermissionRationale = false, showDataAndTimePicker = false) }
            }
            is AddEditNoteEvent.CancelReminder -> {
                _uiState.update { it.copy(reminderTime = -1, showAlarmPermissionRationale = false, showDataAndTimePicker = false) }
                currentNoteId?.let { reminderScheduler.cancel(it) }
            }
            else -> {}
        }
    }

    private fun deleteNote() {
        viewModelScope.launch {
            fileIOUseCases.deleteFiles(_uiState.value.attachedImages.map { it.toString() })
            fileIOUseCases.deleteFiles(_uiState.value.attachedAudios.map { it.uri.toString() })
            currentNoteId?.let { noteUseCases.deleteNote(it) }
            _eventFlow.emit(UiEvent.DeleteNote(currentNoteId))
        }
    }

    private fun handleRecording() {
        viewModelScope.launch {
            if (uiState.value.isRecording) {
                audioRecorder.stopRecording()
                _uiState.update { it.copy(isRecording = false) }
                currentRecordingFile?.let { file ->
                    val uri = Uri.fromFile(file)
                    val duration = audioMetadataProvider.getDuration(uri)
                    _uiState.update { it.copy(
                        attachedAudios = it.attachedAudios + Attachment(uri, duration)
                    )}
                }
            } else {
                val file = fileIOUseCases.createFile(AttachmentType.AUDIO.extension, AttachmentType.AUDIO.name.lowercase())
                currentRecordingFile = file
                file?.let {
                    audioRecorder.startRecording(it)
                    _uiState.update { state -> state.copy(isRecording = true) }
                }
            }
        }
    }

    private fun saveNote(id:Long?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = uiState.value

            noteUseCases.saveNoteWithAttachments(
                id = id,
                title = state.titleState.text,
                content = state.contentState.text,
                timestamp = System.currentTimeMillis(),
                color = state.noteColor,
                imageUris = state.attachedImages,
                audioUris = state.attachedAudios.map { it.uri },
                reminderTime = state.reminderTime
            ).onSuccess { newNoteId ->
                reminderScheduler.cancel(id = newNoteId)
                reminderScheduler.schedule(id = newNoteId, title = state.titleState.text, content = state.contentState.text, reminderTime = state.reminderTime)
                _eventFlow.emit(if(id!=newNoteId) UiEvent.MakeCopy(newNoteId) else UiEvent.SaveNote)
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false) }
                _eventFlow.emit(UiEvent.ShowSnackbar(e.message ?: "Save Failed"))
            }
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
    }

    sealed class UiEvent{
        data class ShowSnackbar(val message:String):UiEvent()
        object SaveNote: UiEvent()
        data class DeleteNote(val id: Long?): UiEvent()
        data class MakeCopy(val newNoteId: Long): UiEvent()
        object LaunchAudioRecorder : UiEvent()
        data class LaunchCamera(val type: AttachmentType) : UiEvent()
    }
}