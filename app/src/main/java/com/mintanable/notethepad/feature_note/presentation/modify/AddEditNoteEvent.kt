package com.mintanable.notethepad.feature_note.presentation.modify

import android.net.Uri
import androidx.compose.ui.focus.FocusState

sealed class AddEditNoteEvent{
    data class EnteredTitle(val value:String): AddEditNoteEvent()
    data class ChangeTitleFocus(val focusState: FocusState): AddEditNoteEvent()
    data class EnteredContent(val value:String): AddEditNoteEvent()
    data class ChangeContentFocus(val focusState: FocusState): AddEditNoteEvent()
    data class ChangeColor(val color:Int): AddEditNoteEvent()
    object SaveNote: AddEditNoteEvent()
    data class AttachImage(val uri: Uri): AddEditNoteEvent()
    data class RemoveImage(val uri: Uri): AddEditNoteEvent()
}