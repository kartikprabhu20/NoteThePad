package com.mintanable.notethepad.feature_note.presentation.notes

import androidx.compose.runtime.Stable
import com.mintanable.notethepad.feature_note.domain.model.DetailedNote

@Stable
data class NotesState(
    val notes: List<DetailedNote> = emptyList(),
)
