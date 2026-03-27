package com.mintanable.notethepad.core.richtext.model

enum class SpanType(val jsonKey: String) {
    BOLD("bold"),
    ITALIC("italic"),
    UNDERLINE("underline"),
    H1("h1"),
    H2("h2"),
    PARAGRAPH("paragraph"),
    BULLET("bullet");

    companion object {
        fun fromJsonKey(key: String): SpanType? = entries.firstOrNull { it.jsonKey == key }
    }
}
