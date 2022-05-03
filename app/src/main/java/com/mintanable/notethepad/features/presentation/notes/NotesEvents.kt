package com.mintanable.notethepad.features.presentation.notes

import com.mintanable.notethepad.features.domain.model.Note
import com.mintanable.notethepad.features.domain.util.NoteOrder

sealed class NotesEvent{
    data class Order(val noteOrder: NoteOrder): NotesEvent()
    data class DeleteNote(val note:Note): NotesEvent()
    object RestoreNote: NotesEvent()
    object ToggleOrderSection: NotesEvent()
}
