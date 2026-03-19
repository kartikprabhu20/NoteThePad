package com.mintanable.notethepad.feature_note.presentation.notes

import com.mintanable.notethepad.core.model.DetailedNote
import com.mintanable.notethepad.core.model.NoteOrder
import com.mintanable.notethepad.core.model.Tag

sealed class NotesEvent {
    data class Order(val noteOrder: NoteOrder) : NotesEvent()
    data class DeleteNote(val detailedNote: DetailedNote) : NotesEvent()
    data object RestoreNote : NotesEvent()
    data object ToggleOrderSection : NotesEvent()
    data class SearchBarValueChange(val searchQuery: String) : NotesEvent()
    data class PinNote(val detailedNote: DetailedNote) : NotesEvent()
    data object ShowLabelDialog : NotesEvent()
    data object DismissLabelDialog : NotesEvent()
    data class AddLabel(val tagName: String) : NotesEvent()
    data class EditLabel(val tag: Tag) : NotesEvent()
    data class DeleteLabel(val tag: Tag) : NotesEvent()
}
