package com.mintanable.notethepad.features.domain.util

data class NoteTextFieldState(
    val text: String = "",
    val hint : String = "",
    val isHintVisible: Boolean =  true
)

