package com.mintanable.notethepad.core.richtext.model

data class RichSpan(
    val type: SpanType,
    val start: Int,
    val end: Int
) {
    init {
        require(start >= 0) { "start must be >= 0, got $start" }
        require(end >= start) { "end must be >= start, got end=$end start=$start" }
    }

    fun isEmpty(): Boolean = start == end
    fun overlaps(other: RichSpan): Boolean = start < other.end && end > other.start
    fun contains(offset: Int): Boolean = offset in start until end
}
