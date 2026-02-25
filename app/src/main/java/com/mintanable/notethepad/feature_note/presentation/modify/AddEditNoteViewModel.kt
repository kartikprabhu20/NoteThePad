package com.mintanable.notethepad.feature_note.presentation.modify

import android.util.Log
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintanable.notethepad.core.file.FileManager
import com.mintanable.notethepad.feature_note.domain.model.InvalidNoteException
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.model.NoteColors
import com.mintanable.notethepad.feature_note.domain.use_case.NoteUseCases
import com.mintanable.notethepad.feature_note.domain.util.AttachmentType
import com.mintanable.notethepad.feature_note.domain.util.NoteTextFieldState
import com.mintanable.notethepad.feature_note.presentation.notes.util.AttachmentHelper
import com.mintanable.notethepad.feature_settings.presentation.use_cases.GetCameraPermissionFlag
import com.mintanable.notethepad.feature_settings.presentation.use_cases.MarkCameraPermissionFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditNoteViewModel @Inject constructor(
    private val noteUseCases: NoteUseCases,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext val context: Context,
    private val fileManager: FileManager,
    private val markCameraPermissionFlag: MarkCameraPermissionFlag,
    private val getCameraPermissionFlag: GetCameraPermissionFlag,
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

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _attachedImages = MutableStateFlow<List<Uri>>(emptyList())
    val attachedImageUris = _attachedImages.asStateFlow()

    private var currentNoteId: Int? = null

    suspend fun hasAskedForCameraPermissionBefore(): Boolean {
        return getCameraPermissionFlag()
    }

    fun markCameraPermissionRequested() {
        viewModelScope.launch {
            markCameraPermissionFlag()
        }
    }

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
                _attachedImages.value = note.imageUris.map {
                    Log.d("kptest", "load: $it")
                    it.toUri()
                }
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
                    try{
                        noteUseCases.addNote(
                            Note(
                                title = noteTitle.value.text,
                                content = noteContent.value.text,
                                timestamp = System.currentTimeMillis(),
                                color = noteColor.value,
                                id = currentNoteId,
                                imageUris = attachedImageUris.value.mapNotNull { uri ->
                                    val uri = uri
                                    if (uri.toString().contains(context.packageName)) {
                                        uri.toString()
                                    } else {
                                        fileManager.saveMediaToStorage(uri, AttachmentHelper.getAttachmentType(context, uri).name.lowercase())
                                    }
                                }
                            )
                        )
                        _eventFlow.emit(UiEvent.SaveNote)
                    }catch(e: InvalidNoteException){
                        _eventFlow.emit(
                            UiEvent.ShowSnackbar(
                                message = e.message?: "Could not save note"
                            )
                        )
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
                viewModelScope.launch { fileManager.deleteFileFromUris(listOf(event.uri)) }
            }

            is AddEditNoteEvent.AttachVideo -> {
                _attachedImages.update { current ->
                    if (current.contains(event.uri)) current else current + event.uri
                }
            }

            else -> {}
        }
    }

    fun generateTempUri(attachmentType: AttachmentType): Uri? {
        return fileManager.createTempUri(attachmentType.extension)
    }


    sealed class UiEvent{
        data class ShowSnackbar(val message:String):UiEvent()
        object SaveNote: UiEvent()
    }
}