package com.mintanable.notethepad.feature_note.presentation.notes

import com.mintanable.notethepad.core.model.note.DetailedNote
import com.mintanable.notethepad.core.model.note.NoteOrder
import com.mintanable.notethepad.core.model.note.TagEntity

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
    data class EditLabel(val tagEntity: TagEntity) : NotesEvent()
    data class DeleteLabel(val tagEntity: TagEntity) : NotesEvent()
}
