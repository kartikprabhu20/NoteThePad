package com.mintanable.notethepad.core.richtext.engine

import com.mintanable.notethepad.core.richtext.model.RichSpan
import com.mintanable.notethepad.core.richtext.model.RichTextDocument
import com.mintanable.notethepad.core.richtext.model.SpanType

object SpanAdjustmentEngine {

    val BLOCK_TYPES = setOf(SpanType.H1, SpanType.H2, SpanType.PARAGRAPH, SpanType.BULLET)

    // ── Text mutation ──────────────────────────────────────────────────────

    /**
     * Updates span offsets after a text change.
     *
     * [changeStart] is where the edit begins in the OLD text.
     * [changeEnd]   is the exclusive end of the deleted region in the OLD text
     *               (equals [changeStart] for pure insertion).
     */
    fun adjustForTextChange(
        doc: RichTextDocument,
        newText: String,
        changeStart: Int,
        changeEnd: Int
    ): RichTextDocument {
        val delta = newText.length - doc.rawText.length
        val adjusted = doc.spans.mapNotNull { adjustSpan(it, changeStart, changeEnd, delta) }
        return RichTextDocument.of(newText, adjusted)
    }

    private fun adjustSpan(span: RichSpan, changeStart: Int, changeEnd: Int, delta: Int): RichSpan? {
        return when {
            // Span ends at or before the change point: unchanged
            span.end <= changeStart -> span

            // Span starts at or after the deleted region: shift
            span.start >= changeEnd -> span.copy(
                start = span.start + delta,
                end = span.end + delta
            )

            // Span fully encloses the change region: expand/contract
            span.start <= changeStart && span.end >= changeEnd -> {
                val newEnd = span.end + delta
                if (newEnd <= span.start) null else span.copy(end = newEnd)
            }

            // Span overlaps only the left part of the change region
            span.start < changeStart && span.end <= changeEnd -> {
                if (changeStart <= span.start) null else span.copy(end = changeStart)
            }

            // Span overlaps only the right part of the change region
            span.start >= changeStart && span.start < changeEnd -> {
                val newStart = changeStart + if (changeStart == changeEnd) delta else 0
                val newEnd = span.end + delta
                if (newEnd <= newStart) null else span.copy(start = newStart, end = newEnd)
            }

            else -> span
        }
    }

    // ── Span toggle ────────────────────────────────────────────────────────

    /**
     * Toggles an inline format type over [selectionStart]..[selectionEnd].
     * If fully active → removes it; otherwise adds it.
     */
    fun toggleSpan(
        doc: RichTextDocument,
        type: SpanType,
        selectionStart: Int,
        selectionEnd: Int
    ): RichTextDocument {
        if (selectionStart >= selectionEnd) return doc
        val isActive = doc.isActiveAt(type, selectionStart, selectionEnd)
        val newSpans = if (isActive) {
            removeSpanFromRange(doc.spans, type, selectionStart, selectionEnd)
        } else {
            addSpanToRange(doc.spans, type, selectionStart, selectionEnd)
        }
        return RichTextDocument.of(doc.rawText, newSpans)
    }

    // ── Block type apply ───────────────────────────────────────────────────

    /**
     * Applies a block-level type (H1, H2, PARAGRAPH, BULLET) to every line
     * that the selection touches. Replaces any conflicting block type on those lines.
     * Calling applyBlockType with the already-active type removes it (toggle behaviour).
     */
    fun applyBlockType(
        doc: RichTextDocument,
        type: SpanType,
        selectionStart: Int,
        selectionEnd: Int
    ): RichTextDocument {
        val lineRanges = getLineRanges(doc.rawText, selectionStart, selectionEnd)
        var current = doc
        for ((lineStart, lineEnd) in lineRanges) {
            // Check if this type is already fully active on this line
            val isActive = current.isActiveAt(type, lineStart, lineEnd)
            // Remove all block types from this line
            var spans = current.spans
            for (blockType in BLOCK_TYPES) {
                spans = removeSpanFromRange(spans, blockType, lineStart, lineEnd)
            }
            // Add the requested type unless it was already active (toggle off)
            if (!isActive) {
                spans = addSpanToRange(spans, type, lineStart, lineEnd)
            }
            current = RichTextDocument.of(current.rawText, spans)
        }
        return current
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun removeSpanFromRange(
        spans: List<RichSpan>,
        type: SpanType,
        start: Int,
        end: Int
    ): List<RichSpan> {
        val result = mutableListOf<RichSpan>()
        for (span in spans) {
            if (span.type != type || !span.overlaps(RichSpan(type, start, end))) {
                result.add(span)
                continue
            }
            if (span.start < start) result.add(span.copy(end = start))
            if (span.end > end) result.add(span.copy(start = end))
        }
        return result
    }

    private fun addSpanToRange(
        spans: List<RichSpan>,
        type: SpanType,
        start: Int,
        end: Int
    ): List<RichSpan> {
        var mergedStart = start
        var mergedEnd = end
        val others = mutableListOf<RichSpan>()
        for (span in spans) {
            if (span.type == type && span.start <= end && span.end >= start) {
                mergedStart = minOf(mergedStart, span.start)
                mergedEnd = maxOf(mergedEnd, span.end)
            } else {
                others.add(span)
            }
        }
        others.add(RichSpan(type, mergedStart, mergedEnd))
        return others
    }

    fun getLineRanges(text: String, selStart: Int, selEnd: Int): List<Pair<Int, Int>> {
        if (text.isEmpty()) return listOf(0 to 0)
        val ranges = mutableListOf<Pair<Int, Int>>()
        var lineStart = 0
        text.forEachIndexed { i, c ->
            if (c == '\n' || i == text.lastIndex) {
                val lineEnd = if (c == '\n') i + 1 else i + 1
                val clampedSelEnd = if (selEnd == selStart) selStart + 1 else selEnd
                if (lineEnd > selStart && lineStart < clampedSelEnd) {
                    ranges.add(lineStart to lineEnd)
                }
                lineStart = i + 1
            }
        }
        return ranges
    }
}
