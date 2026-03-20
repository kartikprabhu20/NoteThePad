package com.mintanable.notethepad.feature_note.presentation.modify

import android.net.Uri
import androidx.compose.ui.focus.FocusState
import com.mintanable.notethepad.core.model.note.CheckboxItem
import com.mintanable.notethepad.feature_note.presentation.notes.BottomSheetType

sealed class AddEditNoteEvent {
    data class EnteredTitle(val value: String) : AddEditNoteEvent()
    data class ChangeTitleFocus(val focusState: FocusState) : AddEditNoteEvent()
    data class EnteredContent(val value: String) : AddEditNoteEvent()
    data class ChangeContentFocus(val focusState: FocusState) : AddEditNoteEvent()
    data class ChangeColor(val color: Int) : AddEditNoteEvent()
    data object SaveNote : AddEditNoteEvent()
    data object MakeCopy : AddEditNoteEvent()
    data object DeleteNote : AddEditNoteEvent()
    data class AttachImage(val uri: Uri) : AddEditNoteEvent()
    data class RemoveImage(val uri: Uri) : AddEditNoteEvent()
    data class AttachVideo(val uri: Uri) : AddEditNoteEvent()
    data class RemoveAudio(val uri: String) : AddEditNoteEvent()
    data object ToggleAudioRecording : AddEditNoteEvent()
    data object DismissDialogs : AddEditNoteEvent()
    data class UpdateSheetType(val sheetType: BottomSheetType) : AddEditNoteEvent()
    data class ToggleZoom(val uri: Uri) : AddEditNoteEvent()
    data class UpdateNowPlaying(val uri: String) : AddEditNoteEvent()
    data object StopMedia : AddEditNoteEvent()
    data class SetReminder(val timestamp: Long) : AddEditNoteEvent()
    data object CancelReminder : AddEditNoteEvent()
    data object CheckAlarmPermission : AddEditNoteEvent()
    data object DismissReminder : AddEditNoteEvent()
    data object ToggleCheckbox : AddEditNoteEvent()
    data class UpdateCheckList(val list: List<CheckboxItem>) : AddEditNoteEvent()
    data class AddChecklistItem(val previousCheckItem: CheckboxItem) : AddEditNoteEvent()
    data object PinNote : AddEditNoteEvent()
    data object ShowLabelDialog : AddEditNoteEvent()
    data class InsertLabel(val tagName: String) : AddEditNoteEvent()
    data class DeleteLabel(val tagName: String) : AddEditNoteEvent()
    data object ShowSuggestions : AddEditNoteEvent()
    data object ClearSuggestions : AddEditNoteEvent()
}
