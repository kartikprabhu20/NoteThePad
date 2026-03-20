package com.mintanable.notethepad.feature_note.presentation.notes

import com.mintanable.notethepad.database.db.entity.DetailedNote

data class NotesState(
    val notes: List<DetailedNote> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
