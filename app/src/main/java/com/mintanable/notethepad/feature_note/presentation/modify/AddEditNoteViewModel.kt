package com.mintanable.notethepad.feature_note.presentation.modify

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintanable.notethepad.feature_note.domain.model.NoteColors
import com.mintanable.notethepad.feature_note.domain.repository.AudioRecorder
import com.mintanable.notethepad.feature_note.domain.use_case.FileIOUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.NoteUseCases
import com.mintanable.notethepad.feature_note.domain.util.AttachmentType
import com.mintanable.notethepad.feature_note.presentation.notes.NoteTextFieldState
import com.mintanable.notethepad.feature_settings.presentation.use_cases.PermissionUsecases
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val fileIOUseCases: FileIOUseCases
): ViewModel(){

    private val passedNoteId: Int = savedStateHandle.get<Int>("noteId") ?: -1
    private val isEditMode = passedNoteId != -1

    private val _noteTitle = mutableStateOf(NoteTextFieldState(hint = "Enter title..."))
    val noteTitle : State<NoteTextFieldState> = _noteTitle

    private val _noteContent = mutableStateOf(NoteTextFieldState(hint = "Enter some content..."))
    val noteContent : State<NoteTextFieldState> = _noteContent

    private val _noteColor = mutableStateOf(
        if (isEditMode)
            savedStateHandle.get<Int>("noteColor")
                ?: NoteColors.colors.random().toArgb()
        else
            NoteColors.colors.random().toArgb()
    )
    val noteColor: State<Int> = _noteColor

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _attachedImages = MutableStateFlow<List<Uri>>(emptyList())
    val attachedImageUris = _attachedImages.asStateFlow()

    private val _attachedAudioUris = MutableStateFlow<List<Uri>>(emptyList())
    val attachedAudioUris = _attachedAudioUris.asStateFlow()

    private var currentNoteId: Int? = null

    suspend fun hasAskedForCameraPermissionBefore(): Boolean {
        return permissionUsecases.getCameraPermissionFlag()
    }

    fun markCameraPermissionRequested() {
        viewModelScope.launch {
            permissionUsecases.markCameraPermissionFlag()
        }
    }

    suspend fun hasAskedForMicrophonePermissionBefore(): Boolean {
        return permissionUsecases.getMicrophonePermissionFlag()
    }

    fun markMicrophonePermissionRequested() {
        viewModelScope.launch {
            permissionUsecases.markMicrophonePermissionFlag()
        }
    }

    private var currentRecordingFile: File? = null
    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    init {
        if (isEditMode) {
            loadNote(passedNoteId)
        }
    }

    private fun loadNote(id: Int) {
        viewModelScope.launch {
            noteUseCases.getNote(id)?.also { note ->
                currentNoteId = note.id
                _noteTitle.value = _noteTitle.value.copy(text = note.title, isHintVisible = false)
                _noteContent.value = _noteContent.value.copy(text = note.content, isHintVisible = false)
                _noteColor.value = note.color
                _attachedImages.value = note.imageUris.map { it.toUri() }
                _attachedAudioUris.value = note.audioUris.map { it.toUri() }
            }
        }
    }

    fun onEvent(event:AddEditNoteEvent){
        when(event){
            is AddEditNoteEvent.EnteredTitle->{
                _noteTitle.value = noteTitle.value.copy(
                    text = event.value
                )
            }
            is AddEditNoteEvent.ChangeTitleFocus -> {
                _noteTitle.value = noteTitle.value.copy(
                    isHintVisible = !event.focusState.isFocused &&
                            noteTitle.value.text.isBlank()
                )
            }

            is AddEditNoteEvent.EnteredContent->{
                _noteContent.value = noteContent.value.copy(
                    text = event.value
                )
            }
            is AddEditNoteEvent.ChangeContentFocus -> {
                _noteContent.value = noteContent.value.copy(
                    isHintVisible = !event.focusState.isFocused &&
                            noteContent.value.text.isBlank()
                )
            }

            is AddEditNoteEvent.ChangeColor -> {
                _noteColor.value = event.color
            }

            is AddEditNoteEvent.SaveNote ->{
                viewModelScope.launch {
                    _isSaving.value = true

                    noteUseCases.saveNoteWithAttachments(
                        id = currentNoteId,
                        title = noteTitle.value.text,
                        content = noteContent.value.text,
                        timestamp = System.currentTimeMillis(),
                        color = noteColor.value,
                        imageUris = attachedImageUris.value,
                        audioUris = attachedAudioUris.value
                    ).onSuccess {
                        _eventFlow.emit(UiEvent.SaveNote)
                    }.onFailure { e ->
                        _isSaving.value = false
                        _eventFlow.emit(UiEvent.ShowSnackbar(e.message ?: "Save Failed"))
                    }
                }
            }

            is AddEditNoteEvent.AttachImage -> {
                _attachedImages.update { current ->
                    if (current.contains(event.uri)) current else current + event.uri
                }
            }

            is AddEditNoteEvent.RemoveImage -> {
                _attachedImages.update { it - event.uri }
                viewModelScope.launch { fileIOUseCases.deleteFiles(listOf(event.uri.toString())) }
            }

            is AddEditNoteEvent.AttachVideo -> {
                _attachedImages.update { current ->
                    if (current.contains(event.uri)) current else current + event.uri
                }
            }

            is AddEditNoteEvent.ToggleAudioRecording -> {
                viewModelScope.launch {
                    if (_isRecording.value) {
                        // STOP
                        audioRecorder.stopRecording()
                        _isRecording.value = false
                        currentRecordingFile?.let { file ->
                            val uri = Uri.fromFile(file)
                            _attachedAudioUris.update { it + uri }
                        }
                    } else {
                        // START
                        val file = fileIOUseCases.createTempFile(".mp4")
                        currentRecordingFile = file
                        file?.let {
                            audioRecorder.startRecording(it)
                            _isRecording.value = true
                        }
                    }
                }
            }

            is AddEditNoteEvent.RemoveAudio -> {
                Log.d("kptest", "RemoveAudio ${event.uri} ${_attachedAudioUris.value}")
                _attachedAudioUris.update { it - event.uri }
            }

            else -> {}
        }
    }

    fun generateTempUri(attachmentType: AttachmentType): Uri? {
        return fileIOUseCases.createTempUri(attachmentType.extension)
    }

    fun checkMicrophonePermission(
        isGranted: Boolean,
        shouldShowRationale: Boolean
    ) {
        viewModelScope.launch {
            val hasAskedBefore = permissionUsecases.getMicrophonePermissionFlag()

            when {
                isGranted -> {
                    _eventFlow.emit(UiEvent.LaunchAudioRecorder)
                }
                shouldShowRationale -> {
                    _eventFlow.emit(UiEvent.ShowAudioRationale)
                }
                hasAskedBefore -> {
                    _eventFlow.emit(UiEvent.OpenMicrophoneSettings)
                }
                else -> {
                    permissionUsecases.markMicrophonePermissionFlag()
                }
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
                isGranted -> {
                    _eventFlow.emit(UiEvent.LaunchCamera(attachmentType))
                }
                shouldShowRationale -> {
                    _eventFlow.emit(UiEvent.ShowCameraRationale)
                }
                hasAskedBefore -> {
                    _eventFlow.emit(UiEvent.OpenCameraSettings)
                }
                else -> {
                    permissionUsecases.markCameraPermissionFlag()
                }
            }
        }
    }

    sealed class UiEvent{
        data class ShowSnackbar(val message:String):UiEvent()
        object SaveNote: UiEvent()
        object ShowAudioRationale : UiEvent()
        object ShowCameraRationale : UiEvent()
        object OpenCameraSettings : UiEvent()
        object OpenMicrophoneSettings : UiEvent()
        object LaunchAudioRecorder : UiEvent()
        data class LaunchCamera(val type: AttachmentType) : UiEvent()
    }
}