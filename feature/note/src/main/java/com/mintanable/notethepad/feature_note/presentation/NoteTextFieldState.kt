package com.mintanable.notethepad.feature_note.presentation

import androidx.compose.runtime.Stable
import com.mintanable.notethepad.core.richtext.model.RichTextDocument

@Stable
data class NoteTextFieldState(
    val richText: RichTextDocument = RichTextDocument.EMPTY,
    val isFocused: Boolean = false,
    val hint: String = "",
    val isHintVisible: Boolean = true
)
