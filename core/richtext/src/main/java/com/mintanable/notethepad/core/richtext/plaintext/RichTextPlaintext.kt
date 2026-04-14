package com.mintanable.notethepad.core.richtext.plaintext

import com.mintanable.notethepad.core.richtext.serializer.RichTextSerializer

fun extractPlaintext(stored: String): String {
    if (stored.isBlank()) return ""
    return RichTextSerializer.deserialize(stored).rawText
}
