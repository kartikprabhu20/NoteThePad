package com.mintanable.notethepad.feature_note.presentation.modify

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintanable.notethepad.feature_note.data.repository.AudioMetadataProvider
import com.mintanable.notethepad.feature_note.domain.AddEditNoteUiState
import com.mintanable.notethepad.feature_note.domain.model.NoteColors
import com.mintanable.notethepad.feature_note.domain.repository.AudioPlayer
import com.mintanable.notethepad.feature_note.domain.repository.AudioRecorder
import com.mintanable.notethepad.feature_note.domain.use_case.FileIOUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.NoteUseCases
import com.mintanable.notethepad.feature_note.domain.util.AttachmentType
import com.mintanable.notethepad.feature_note.domain.util.AudioAttachment
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
    private val audioPlayer: AudioPlayer,
    private val audioMetadataProvider: AudioMetadataProvider
): ViewModel(){

    private val passedNoteId: Int = savedStateHandle.get<Int>("noteId") ?: -1
    private val isEditMode = passedNoteId != -1
    private var currentNoteId: Int? = null
    private var currentRecordingFile: File? = null

    private val _uiState = MutableStateFlow(
        AddEditNoteUiState(
            noteColor = if (isEditMode) {
                savedStateHandle.get<Int>("noteColor") ?: NoteColors.colors.random().toArgb()
            } else NoteColors.colors.random().toArgb()
        )
    )

    val uiState: StateFlow<AddEditNoteUiState> = combine(
        _uiState,
        audioPlayer.audioState
    ) { state, audioState ->
//        Log.d("kptest", "state: $state audioState: $audioState")
        state.copy(audioState = audioState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddEditNoteUiState()
    )

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        if (isEditMode) {
            loadNote(passedNoteId)
        }
    }

    private fun loadNote(id: Int) {
        viewModelScope.launch {
            noteUseCases.getNote(id)?.also { note ->
                currentNoteId = note.id
                val audioAttachments = note.audioUris.map { uriString ->
                    val uri = uriString.toUri()
                    AudioAttachment(uri, audioMetadataProvider.getDuration(uri))
                }

                _uiState.update {
                    it.copy(
                        titleState = it.titleState.copy(text = note.title, isHintVisible = false),
                        contentState = it.contentState.copy(text = note.content, isHintVisible = false),
                        noteColor = note.color,
                        attachedImages = note.imageUris.map { it.toUri() },
                        attachedAudios = audioAttachments
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
                saveNote()
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
                if (uiState.value.audioState?.currentUri == event.uri.toString()) {
                    audioPlayer.stop()
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
                _uiState.update { it.copy(zoomedImageUri = event.uri) }
            }
            is AddEditNoteEvent.UpdateNowPlaying -> {
                audioPlayer.playPause(event.uri)
            }
            is AddEditNoteEvent.StopAudio -> {
                audioPlayer.stop()
            }
            else -> {}
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
                        attachedAudios = it.attachedAudios + AudioAttachment(uri, duration)
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

    private fun saveNote() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = uiState.value

            noteUseCases.saveNoteWithAttachments(
                id = currentNoteId,
                title = state.titleState.text,
                content = state.contentState.text,
                timestamp = System.currentTimeMillis(),
                color = state.noteColor,
                imageUris = state.attachedImages,
                audioUris = state.attachedAudios.map { it.uri }
            ).onSuccess {
                _eventFlow.emit(UiEvent.SaveNote)
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

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }

    sealed class UiEvent{
        data class ShowSnackbar(val message:String):UiEvent()
        object SaveNote: UiEvent()
        object LaunchAudioRecorder : UiEvent()
        data class LaunchCamera(val type: AttachmentType) : UiEvent()
    }
}