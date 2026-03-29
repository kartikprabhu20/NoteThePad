package com.mintanable.notethepad.core.richtext.model

import androidx.compose.ui.text.input.TextFieldValue

data class RichTextState(
    val document: RichTextDocument = RichTextDocument.EMPTY,
    val textFieldValue: TextFieldValue = TextFieldValue(),
    val pendingStyles: Set<SpanType> = emptySet(),
    val pendingBlockType: SpanType? = null,
    val pendingBullet: Boolean = false
) {
    val activeStyles: Set<SpanType>
        get() = pendingStyles +
                setOfNotNull(pendingBlockType) +
                (if (pendingBullet) setOf(SpanType.BULLET) else emptySet())

    companion object {
        val EMPTY = RichTextState()
    }
}
