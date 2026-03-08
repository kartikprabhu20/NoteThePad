package com.mintanable.notethepad.feature_note.presentation.notes

import com.mintanable.notethepad.feature_note.domain.model.DetailedNote
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.util.NoteOrder

sealed class NotesEvent{
    data class Order(val noteOrder: NoteOrder): NotesEvent()
    data class DeleteNote(val detailedNote: DetailedNote): NotesEvent()
    data object RestoreNote: NotesEvent()
    data object ToggleOrderSection: NotesEvent()
    data class SearchBarValueChange(val searchQuery:String) : NotesEvent()
    data class PinNote(val detailedNote: DetailedNote): NotesEvent()
}
