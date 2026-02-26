package com.mintanable.notethepad.feature_note.presentation.notes

data class NoteTextFieldState(
    val text: String = "",
    val hint : String = "",
    val isHintVisible: Boolean =  true
)

