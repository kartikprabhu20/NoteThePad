package com.mintanable.notethepad.feature_note.presentation.modify

import android.net.Uri
import androidx.compose.ui.focus.FocusState
import com.mintanable.notethepad.feature_note.presentation.notes.BottomSheetType

sealed class AddEditNoteEvent{
    data class EnteredTitle(val value:String): AddEditNoteEvent()
    data class ChangeTitleFocus(val focusState: FocusState): AddEditNoteEvent()
    data class EnteredContent(val value:String): AddEditNoteEvent()
    data class ChangeContentFocus(val focusState: FocusState): AddEditNoteEvent()
    data class ChangeColor(val color:Int): AddEditNoteEvent()
    object SaveNote: AddEditNoteEvent()
    object MakeCopy: AddEditNoteEvent()
    data class AttachImage(val uri: Uri): AddEditNoteEvent()
    data class RemoveImage(val uri: Uri): AddEditNoteEvent()
    data class AttachVideo(val uri: Uri): AddEditNoteEvent()
    data class RemoveAudio(val uri: Uri): AddEditNoteEvent()
    object ToggleAudioRecording: AddEditNoteEvent()
    object DismissDialogs: AddEditNoteEvent()
    data class UpdateSheetType(val sheetType: BottomSheetType): AddEditNoteEvent()
    data class ToggleZoom(val uri: Uri): AddEditNoteEvent()
    data class UpdateNowPlaying(val uri: Uri): AddEditNoteEvent()
    object StopMedia: AddEditNoteEvent()
}