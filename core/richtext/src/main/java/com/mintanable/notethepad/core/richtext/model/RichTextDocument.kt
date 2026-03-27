package com.mintanable.notethepad.core.richtext.model

data class RichTextDocument(
    val rawText: String = "",
    val spans: List<RichSpan> = emptyList()
) {
    companion object {
        val EMPTY = RichTextDocument()

        fun of(rawText: String, spans: List<RichSpan>): RichTextDocument =
            RichTextDocument(
                rawText,
                spans.filter { !it.isEmpty() }.sortedWith(compareBy({ it.start }, { it.type.ordinal }))
            )
    }

    /** Returns true if [type] covers the entire range [start, end). */
    fun isActiveAt(type: SpanType, start: Int, end: Int): Boolean =
        spans.any { it.type == type && it.start <= start && it.end >= end }

    /** Returns all span types active at a cursor [offset]. */
    fun activeTypesAt(offset: Int): Set<SpanType> =
        spans.filter { it.contains(offset) }.mapTo(mutableSetOf()) { it.type }
}
