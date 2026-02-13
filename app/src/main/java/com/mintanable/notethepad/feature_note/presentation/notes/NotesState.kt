package com.mintanable.notethepad.features.presentation.notes

import com.mintanable.notethepad.features.domain.model.Note
import com.mintanable.notethepad.features.domain.util.NoteOrder
import com.mintanable.notethepad.features.domain.util.OrderType

data class NotesState(
    val notes: List<Note> = emptyList(),
    val noteOrder: NoteOrder = NoteOrder.Date(OrderType.Descending),
    val isOrderSectionVisible : Boolean = false,
)
