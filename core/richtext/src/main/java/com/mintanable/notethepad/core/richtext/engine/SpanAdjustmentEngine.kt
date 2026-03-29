package com.mintanable.notethepad.core.richtext.engine

import com.mintanable.notethepad.core.richtext.model.RichSpan
import com.mintanable.notethepad.core.richtext.model.RichTextDocument
import com.mintanable.notethepad.core.richtext.model.SpanType

object SpanAdjustmentEngine {

    val BLOCK_TYPES = setOf(SpanType.H1, SpanType.H2, SpanType.PARAGRAPH)
    const val BULLET_PREFIX = "• "
    private const val BULLET_PREFIX_LEN = 2

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

    internal fun adjustSpan(span: RichSpan, changeStart: Int, changeEnd: Int, delta: Int): RichSpan? {
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
            span.start < changeStart -> {
                span.copy(end = changeStart)
            }

            // Span overlaps only the right part of the change region
            span.start < changeEnd -> {
                val newEnd = span.end + delta
                if (newEnd <= changeStart) null else span.copy(start = changeStart, end = newEnd)
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
     * Applies a block-level type to the given range. No line expansion — raw range only.
     * Used internally; prefer [applyBlockTypeToLines] for user-facing actions.
     */
    fun applyBlockType(
        doc: RichTextDocument,
        type: SpanType,
        selectionStart: Int,
        selectionEnd: Int
    ): RichTextDocument {
        if (selectionStart >= selectionEnd) return doc
        val isActive = doc.isActiveAt(type, selectionStart, selectionEnd)
        var spans = doc.spans
        for (blockType in BLOCK_TYPES) {
            spans = removeSpanFromRange(spans, blockType, selectionStart, selectionEnd)
        }
        if (!isActive) {
            spans = addSpanToRange(spans, type, selectionStart, selectionEnd)
        }
        return RichTextDocument.of(doc.rawText, spans)
    }

    /**
     * Like [applyBlockType] but only adds — never toggles off.
     * Used during typing to maintain the pending block type on the current line.
     */
    fun ensureBlockType(
        doc: RichTextDocument,
        type: SpanType,
        selectionStart: Int,
        selectionEnd: Int
    ): RichTextDocument {
        if (selectionStart >= selectionEnd) return doc
        val ranges = getLineRanges(doc.rawText, selectionStart, selectionEnd)
        if (ranges.isEmpty()) return doc
        val expandedStart = ranges.first().first
        val expandedEnd = ranges.last().second
        if (expandedStart >= expandedEnd) return doc
        if (doc.isActiveAt(type, expandedStart, expandedEnd)) return doc
        var spans = doc.spans
        for (blockType in BLOCK_TYPES) {
            spans = removeSpanFromRange(spans, blockType, expandedStart, expandedEnd)
        }
        spans = addSpanToRange(spans, type, expandedStart, expandedEnd)
        return RichTextDocument.of(doc.rawText, spans)
    }

    /**
     * Applies a block-level type to all lines touched by [selStart]..[selEnd].
     * Toggle behaviour: if the type is already active on ALL touched lines, it is removed.
     */
    fun applyBlockTypeToLines(
        doc: RichTextDocument,
        type: SpanType,
        selStart: Int,
        selEnd: Int
    ): RichTextDocument {
        val lineRanges = getLineRanges(doc.rawText, selStart, selEnd)
        if (lineRanges.isEmpty()) return doc
        val expandedStart = lineRanges.first().first
        val expandedEnd = lineRanges.last().second
        if (expandedStart >= expandedEnd) return doc

        val allActive = lineRanges.all { (ls, le) ->
            le > ls && doc.isActiveAt(type, ls, le)
        }
        var spans = doc.spans
        for (blockType in BLOCK_TYPES) {
            spans = removeSpanFromRange(spans, blockType, expandedStart, expandedEnd)
        }
        if (!allActive) {
            spans = addSpanToRange(spans, type, expandedStart, expandedEnd)
        }
        return RichTextDocument.of(doc.rawText, spans)
    }

    // ── Bullet toggle (independent of block types) ──────────────────────

    /**
     * Toggles BULLET on the lines touched by [selStart]..[selEnd].
     * Does NOT touch H1/H2/P — BULLET is independent.
     */
    fun toggleBulletOnLines(
        doc: RichTextDocument,
        selStart: Int,
        selEnd: Int
    ): RichTextDocument {
        val lineRanges = getLineRanges(doc.rawText, selStart, selEnd)
        if (lineRanges.isEmpty()) return doc
        val expandedStart = lineRanges.first().first
        val expandedEnd = lineRanges.last().second
        if (expandedStart >= expandedEnd) return doc

        val allActive = lineRanges.all { (ls, le) ->
            le > ls && doc.isActiveAt(SpanType.BULLET, ls, le)
        }
        var spans = removeSpanFromRange(doc.spans, SpanType.BULLET, expandedStart, expandedEnd)
        if (!allActive) {
            spans = addSpanToRange(spans, SpanType.BULLET, expandedStart, expandedEnd)
        }
        return RichTextDocument.of(doc.rawText, spans)
    }

    /**
     * Like [toggleBulletOnLines] but only adds — never toggles off.
     * Used during typing to maintain BULLET on the current line.
     */
    fun ensureBullet(
        doc: RichTextDocument,
        selectionStart: Int,
        selectionEnd: Int
    ): RichTextDocument {
        if (selectionStart >= selectionEnd) return doc
        val ranges = getLineRanges(doc.rawText, selectionStart, selectionEnd)
        if (ranges.isEmpty()) return doc
        val expandedStart = ranges.first().first
        val expandedEnd = ranges.last().second
        if (expandedStart >= expandedEnd) return doc
        if (doc.isActiveAt(SpanType.BULLET, expandedStart, expandedEnd)) return doc
        val spans = addSpanToRange(doc.spans, SpanType.BULLET, expandedStart, expandedEnd)
        return RichTextDocument.of(doc.rawText, spans)
    }

    // ── Bullet prefix management ─────────────────────────────────────────

    /**
     * Inserts "• " at the start of each line in [lineRanges] that doesn't already have it.
     * Returns (new document, cursor delta).
     */
    fun insertBulletPrefixes(
        doc: RichTextDocument,
        lineRanges: List<Pair<Int, Int>>,
        cursorPos: Int
    ): Pair<RichTextDocument, Int> {
        var text = doc.rawText
        var spans = doc.spans
        var cursorDelta = 0
        var offsetAccum = 0

        for ((origStart, _) in lineRanges) {
            val pos = origStart + offsetAccum
            if (pos <= text.length && text.substring(pos).startsWith(BULLET_PREFIX)) continue

            text = text.substring(0, pos) + BULLET_PREFIX + text.substring(pos)
            spans = spans.mapNotNull { adjustSpan(it, pos, pos, BULLET_PREFIX_LEN) }
            if (pos <= cursorPos + cursorDelta) cursorDelta += BULLET_PREFIX_LEN
            offsetAccum += BULLET_PREFIX_LEN
        }
        return Pair(RichTextDocument.of(text, spans), cursorDelta)
    }

    /**
     * Removes "• " from the start of each line in [lineRanges] that has it.
     * Returns (new document, cursor delta).
     */
    fun removeBulletPrefixes(
        doc: RichTextDocument,
        lineRanges: List<Pair<Int, Int>>,
        cursorPos: Int
    ): Pair<RichTextDocument, Int> {
        var text = doc.rawText
        var spans = doc.spans
        var cursorDelta = 0
        var offsetAccum = 0

        for ((origStart, _) in lineRanges) {
            val pos = origStart + offsetAccum
            if (pos + BULLET_PREFIX_LEN > text.length) continue
            if (!text.substring(pos).startsWith(BULLET_PREFIX)) continue

            text = text.substring(0, pos) + text.substring(pos + BULLET_PREFIX_LEN)
            spans = spans.mapNotNull { adjustSpan(it, pos, pos + BULLET_PREFIX_LEN, -BULLET_PREFIX_LEN) }
            if (pos < cursorPos + cursorDelta) cursorDelta -= BULLET_PREFIX_LEN
            offsetAccum -= BULLET_PREFIX_LEN
        }
        return Pair(RichTextDocument.of(text, spans), cursorDelta)
    }

    /**
     * After a newline is typed on a bullet line, inserts "• " at the start of the new line.
     * Returns (new document, extra chars inserted).
     */
    fun continueBulletAfterEnter(
        doc: RichTextDocument,
        newlinePos: Int
    ): Pair<RichTextDocument, Int> {
        val afterNewline = newlinePos + 1
        if (afterNewline > doc.rawText.length) return doc to 0
        if (afterNewline < doc.rawText.length && doc.rawText.substring(afterNewline).startsWith(BULLET_PREFIX)) {
            return doc to 0
        }
        val newText = doc.rawText.substring(0, afterNewline) + BULLET_PREFIX + doc.rawText.substring(afterNewline)
        val newSpans = doc.spans.mapNotNull { adjustSpan(it, afterNewline, afterNewline, BULLET_PREFIX_LEN) }
        return RichTextDocument.of(newText, newSpans) to BULLET_PREFIX_LEN
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    fun removeSpanFromRange(
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

    fun addSpanToRange(
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

    /**
     * Returns line ranges covering the given selection/cursor.
     * Each range is a pair (startInclusive, endExclusive).
     */
    fun getLineRanges(text: String, selStart: Int, selEnd: Int): List<Pair<Int, Int>> {
        if (text.isEmpty()) return listOf(0 to 0)

        // Build all line ranges
        val lines = mutableListOf<Pair<Int, Int>>()
        var lineStart = 0
        for (i in text.indices) {
            if (text[i] == '\n' || i == text.lastIndex) {
                lines.add(lineStart to i + 1)
                lineStart = i + 1
            }
        }
        // Trailing empty line after final '\n'
        if (text.endsWith('\n')) {
            lines.add(text.length to text.length)
        }

        return if (selStart == selEnd) {
            // Cursor mode: find the line containing the cursor position.
            val matches = lines.filter { (ls, le) ->
                if (ls == le) {
                    selStart == ls
                } else {
                    selStart in ls until le || (selStart == le && le == text.length)
                }
            }
            // If cursor lands on an empty trailing line, prefer it over the preceding line
            val hasEmptyLine = matches.any { (ls, le) -> ls == le }
            if (hasEmptyLine) matches.filter { (ls, le) -> ls == le } else matches
        } else {
            // Selection mode: standard overlap
            lines.filter { (ls, le) -> le > selStart && ls < selEnd }
        }
    }
}
