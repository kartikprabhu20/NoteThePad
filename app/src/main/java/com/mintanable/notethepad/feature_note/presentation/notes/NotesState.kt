package com.mintanable.notethepad.feature_note.presentation.notes

import androidx.compose.runtime.Stable
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.util.NoteOrder
import com.mintanable.notethepad.feature_note.domain.util.OrderType

@Stable
data class NotesState(
    val notes: List<Note> = emptyList(),
    val noteOrder: NoteOrder = NoteOrder.Date(OrderType.Descending),
    val isOrderSectionVisible : Boolean = false,
)
