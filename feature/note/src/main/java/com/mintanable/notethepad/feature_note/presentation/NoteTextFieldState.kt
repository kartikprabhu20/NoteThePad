package com.mintanable.notethepad.feature_note.presentation

import androidx.compose.runtime.Stable

@Stable
data class NoteTextFieldState(
    val text: String = "",
    val hint : String = "",
    val isHintVisible: Boolean =  true
)

