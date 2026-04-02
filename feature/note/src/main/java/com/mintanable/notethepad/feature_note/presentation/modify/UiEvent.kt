package com.mintanable.notethepad.feature_note.presentation.modify

import android.content.Intent
import com.mintanable.notethepad.database.db.entity.AttachmentType

sealed class UiEvent{
    data class ShowSnackbar(val message:String):UiEvent()
    data object SaveNote: UiEvent()
    data class DeleteNote(val id: String): UiEvent()
    data class MakeCopy(val newNoteId: String): UiEvent()
    data object LaunchAudioRecorder : UiEvent()
    data class LaunchCamera(val type: AttachmentType) : UiEvent()
    data class RequestWidgetPin(val noteId: String):UiEvent()
    data class ShareNote(val intent: Intent) : UiEvent()
}