package com.mintanable.notethepad.core.richtext.engine

import com.google.common.truth.Truth.assertThat
import com.mintanable.notethepad.core.richtext.model.RichSpan
import com.mintanable.notethepad.core.richtext.model.RichTextDocument
import com.mintanable.notethepad.core.richtext.model.SpanType
import org.junit.Test

class SpanAdjustmentEngineTest {

    // ── adjustSpan ──────────────────────────────────────────────────────

    @Test
    fun `adjustSpan - span before change is unchanged`() {
        val span = RichSpan(SpanType.BOLD, 0, 3)
        val result = SpanAdjustmentEngine.adjustSpan(span, changeStart = 5, changeEnd = 5, delta = 2)
        assertThat(result).isEqualTo(span)
    }

    @Test
    fun `adjustSpan - span after deleted region shifts`() {
        val span = RichSpan(SpanType.BOLD, 10, 15)
        // Delete 2 chars at positions 5-7
        val result = SpanAdjustmentEngine.adjustSpan(span, changeStart = 5, changeEnd = 7, delta = -2)
        assertThat(result).isEqualTo(RichSpan(SpanType.BOLD, 8, 13))
    }

    @Test
    fun `adjustSpan - span after insertion shifts right`() {
        val span = RichSpan(SpanType.BOLD, 10, 15)
        // Insert 3 chars at position 5
        val result = SpanAdjustmentEngine.adjustSpan(span, changeStart = 5, changeEnd = 5, delta = 3)
        assertThat(result).isEqualTo(RichSpan(SpanType.BOLD, 13, 18))
    }

    @Test
    fun `adjustSpan - span enclosing insertion expands`() {
        val span = RichSpan(SpanType.BOLD, 2, 8)
        // Insert 3 chars at position 5 (inside the span)
        val result = SpanAdjustmentEngine.adjustSpan(span, changeStart = 5, changeEnd = 5, delta = 3)
        assertThat(result).isEqualTo(RichSpan(SpanType.BOLD, 2, 11))
    }

    @Test
    fun `adjustSpan - span enclosing deletion contracts`() {
        val span = RichSpan(SpanType.BOLD, 2, 10)
        // Delete 3 chars at positions 4-7
        val result = SpanAdjustmentEngine.adjustSpan(span, changeStart = 4, changeEnd = 7, delta = -3)
        assertThat(result).isEqualTo(RichSpan(SpanType.BOLD, 2, 7))
    }

    @Test
    fun `adjustSpan - span fully deleted returns null`() {
        val span = RichSpan(SpanType.BOLD, 3, 5)
        // Delete the entire region containing the span
        val result = SpanAdjustmentEngine.adjustSpan(span, changeStart = 2, changeEnd = 6, delta = -4)
        assertThat(result).isNull()
    }

    @Test
    fun `adjustSpan - span overlapping left part of change truncates`() {
        val span = RichSpan(SpanType.BOLD, 2, 7)
        // Delete from 5 to 10
        val result = SpanAdjustmentEngine.adjustSpan(span, changeStart = 5, changeEnd = 10, delta = -5)
        assertThat(result).isEqualTo(RichSpan(SpanType.BOLD, 2, 5))
    }

    @Test
    fun `adjustSpan - span overlapping right part of change adjusts`() {
        val span = RichSpan(SpanType.BOLD, 5, 10)
        // Delete from 3 to 7
        val result = SpanAdjustmentEngine.adjustSpan(span, changeStart = 3, changeEnd = 7, delta = -4)
        assertThat(result).isEqualTo(RichSpan(SpanType.BOLD, 3, 6))
    }

    // ── adjustForTextChange ─────────────────────────────────────────────

    @Test
    fun `adjustForTextChange preserves spans before insertion`() {
        val doc = RichTextDocument.of("Hello World", listOf(RichSpan(SpanType.BOLD, 0, 5)))
        val result = SpanAdjustmentEngine.adjustForTextChange(doc, "Hello Beautiful World", 5, 5)
        assertThat(result.rawText).isEqualTo("Hello Beautiful World")
        assertThat(result.spans).contains(RichSpan(SpanType.BOLD, 0, 5))
    }

    @Test
    fun `adjustForTextChange shifts spans after insertion`() {
        val doc = RichTextDocument.of("Hello World", listOf(RichSpan(SpanType.ITALIC, 6, 11)))
        val result = SpanAdjustmentEngine.adjustForTextChange(doc, "Hello Beautiful World", 5, 5)
        assertThat(result.spans).contains(RichSpan(SpanType.ITALIC, 16, 21))
    }

    // ── toggleSpan ──────────────────────────────────────────────────────

    @Test
    fun `toggleSpan adds span when not active`() {
        val doc = RichTextDocument.of("Hello", emptyList())
        val result = SpanAdjustmentEngine.toggleSpan(doc, SpanType.BOLD, 0, 5)
        assertThat(result.isActiveAt(SpanType.BOLD, 0, 5)).isTrue()
    }

    @Test
    fun `toggleSpan removes span when fully active`() {
        val doc = RichTextDocument.of("Hello", listOf(RichSpan(SpanType.BOLD, 0, 5)))
        val result = SpanAdjustmentEngine.toggleSpan(doc, SpanType.BOLD, 0, 5)
        assertThat(result.isActiveAt(SpanType.BOLD, 0, 5)).isFalse()
    }

    @Test
    fun `toggleSpan adds span when partially active`() {
        val doc = RichTextDocument.of("Hello", listOf(RichSpan(SpanType.BOLD, 0, 3)))
        val result = SpanAdjustmentEngine.toggleSpan(doc, SpanType.BOLD, 0, 5)
        assertThat(result.isActiveAt(SpanType.BOLD, 0, 5)).isTrue()
    }

    @Test
    fun `toggleSpan with empty range returns doc unchanged`() {
        val doc = RichTextDocument.of("Hello", listOf(RichSpan(SpanType.BOLD, 0, 5)))
        val result = SpanAdjustmentEngine.toggleSpan(doc, SpanType.BOLD, 3, 3)
        assertThat(result).isEqualTo(doc)
    }

    // ── Block type operations ───────────────────────────────────────────

    @Test
    fun `applyBlockType applies new block type and removes existing`() {
        val doc = RichTextDocument.of("Hello", listOf(RichSpan(SpanType.H1, 0, 5)))
        val result = SpanAdjustmentEngine.applyBlockType(doc, SpanType.H2, 0, 5)
        assertThat(result.isActiveAt(SpanType.H2, 0, 5)).isTrue()
        assertThat(result.isActiveAt(SpanType.H1, 0, 5)).isFalse()
    }

    @Test
    fun `applyBlockType toggles off when same type is already active`() {
        val doc = RichTextDocument.of("Hello", listOf(RichSpan(SpanType.H1, 0, 5)))
        val result = SpanAdjustmentEngine.applyBlockType(doc, SpanType.H1, 0, 5)
        assertThat(result.isActiveAt(SpanType.H1, 0, 5)).isFalse()
    }

    @Test
    fun `ensureBlockType adds block type without toggling off`() {
        val doc = RichTextDocument.of("Hello", listOf(RichSpan(SpanType.H1, 0, 5)))
        val result = SpanAdjustmentEngine.ensureBlockType(doc, SpanType.H1, 0, 5)
        assertThat(result.isActiveAt(SpanType.H1, 0, 5)).isTrue()
    }

    @Test
    fun `ensureBlockType replaces different block type`() {
        val doc = RichTextDocument.of("Hello", listOf(RichSpan(SpanType.H1, 0, 5)))
        val result = SpanAdjustmentEngine.ensureBlockType(doc, SpanType.H2, 0, 5)
        assertThat(result.isActiveAt(SpanType.H2, 0, 5)).isTrue()
        assertThat(result.isActiveAt(SpanType.H1, 0, 5)).isFalse()
    }

    @Test
    fun `applyBlockTypeToLines toggles off when all lines have the type`() {
        val doc = RichTextDocument.of("Line1\nLine2\n", listOf(
            RichSpan(SpanType.H1, 0, 6),
            RichSpan(SpanType.H1, 6, 12)
        ))
        val result = SpanAdjustmentEngine.applyBlockTypeToLines(doc, SpanType.H1, 0, 12)
        assertThat(result.spans.filter { it.type == SpanType.H1 }).isEmpty()
    }

    @Test
    fun `applyBlockTypeToLines applies when not all lines have the type`() {
        val doc = RichTextDocument.of("Line1\nLine2\n", listOf(
            RichSpan(SpanType.H1, 0, 6)
        ))
        val result = SpanAdjustmentEngine.applyBlockTypeToLines(doc, SpanType.H1, 0, 12)
        assertThat(result.isActiveAt(SpanType.H1, 0, 12)).isTrue()
    }

    // ── Bullet operations ───────────────────────────────────────────────

    @Test
    fun `ensureBullet adds bullet span if not present`() {
        val doc = RichTextDocument.of("Hello", emptyList())
        val result = SpanAdjustmentEngine.ensureBullet(doc, 0, 5)
        assertThat(result.isActiveAt(SpanType.BULLET, 0, 5)).isTrue()
    }

    @Test
    fun `ensureBullet does nothing if bullet already present`() {
        val doc = RichTextDocument.of("Hello", listOf(RichSpan(SpanType.BULLET, 0, 5)))
        val result = SpanAdjustmentEngine.ensureBullet(doc, 0, 5)
        assertThat(result.spans.count { it.type == SpanType.BULLET }).isEqualTo(1)
    }

    @Test
    fun `toggleBulletOnLines is independent of block types`() {
        val doc = RichTextDocument.of("Hello", listOf(RichSpan(SpanType.H1, 0, 5)))
        val result = SpanAdjustmentEngine.toggleBulletOnLines(doc, 0, 5)
        assertThat(result.isActiveAt(SpanType.BULLET, 0, 5)).isTrue()
        assertThat(result.isActiveAt(SpanType.H1, 0, 5)).isTrue()
    }

    // ── Bullet prefix management ────────────────────────────────────────

    @Test
    fun `insertBulletPrefixes inserts prefix at line start`() {
        val doc = RichTextDocument.of("Hello", emptyList())
        val lineRanges = listOf(0 to 5)
        val (result, delta) = SpanAdjustmentEngine.insertBulletPrefixes(doc, lineRanges, cursorPos = 3)
        assertThat(result.rawText).isEqualTo("• Hello")
        assertThat(delta).isEqualTo(2)
    }

    @Test
    fun `insertBulletPrefixes skips lines that already have prefix`() {
        val doc = RichTextDocument.of("• Hello", emptyList())
        val lineRanges = listOf(0 to 7)
        val (result, delta) = SpanAdjustmentEngine.insertBulletPrefixes(doc, lineRanges, cursorPos = 5)
        assertThat(result.rawText).isEqualTo("• Hello")
        assertThat(delta).isEqualTo(0)
    }

    @Test
    fun `removeBulletPrefixes removes prefix from line start`() {
        val doc = RichTextDocument.of("• Hello", emptyList())
        val lineRanges = listOf(0 to 7)
        val (result, delta) = SpanAdjustmentEngine.removeBulletPrefixes(doc, lineRanges, cursorPos = 5)
        assertThat(result.rawText).isEqualTo("Hello")
        assertThat(delta).isEqualTo(-2)
    }

    @Test
    fun `removeBulletPrefixes skips lines without prefix`() {
        val doc = RichTextDocument.of("Hello", emptyList())
        val lineRanges = listOf(0 to 5)
        val (result, delta) = SpanAdjustmentEngine.removeBulletPrefixes(doc, lineRanges, cursorPos = 3)
        assertThat(result.rawText).isEqualTo("Hello")
        assertThat(delta).isEqualTo(0)
    }

    @Test
    fun `continueBulletAfterEnter inserts prefix after newline`() {
        val doc = RichTextDocument.of("• Line1\nLine2", emptyList())
        val (result, extra) = SpanAdjustmentEngine.continueBulletAfterEnter(doc, newlinePos = 7)
        assertThat(result.rawText).isEqualTo("• Line1\n• Line2")
        assertThat(extra).isEqualTo(2)
    }

    @Test
    fun `continueBulletAfterEnter skips if prefix already exists`() {
        val doc = RichTextDocument.of("• Line1\n• Line2", emptyList())
        val (result, extra) = SpanAdjustmentEngine.continueBulletAfterEnter(doc, newlinePos = 7)
        assertThat(result.rawText).isEqualTo("• Line1\n• Line2")
        assertThat(extra).isEqualTo(0)
    }

    @Test
    fun `continueBulletAfterEnter at end of document`() {
        val doc = RichTextDocument.of("• Line1\n", emptyList())
        val (result, extra) = SpanAdjustmentEngine.continueBulletAfterEnter(doc, newlinePos = 7)
        assertThat(result.rawText).isEqualTo("• Line1\n• ")
        assertThat(extra).isEqualTo(2)
    }

    // ── getLineRanges ───────────────────────────────────────────────────

    @Test
    fun `getLineRanges with empty text returns single empty range`() {
        val result = SpanAdjustmentEngine.getLineRanges("", 0, 0)
        assertThat(result).containsExactly(0 to 0)
    }

    @Test
    fun `getLineRanges single line no newline`() {
        val result = SpanAdjustmentEngine.getLineRanges("Hello", 0, 0)
        assertThat(result).containsExactly(0 to 5)
    }

    @Test
    fun `getLineRanges two lines`() {
        val text = "Hello\nWorld"
        // Cursor at position 7 (in "World")
        val result = SpanAdjustmentEngine.getLineRanges(text, 7, 7)
        assertThat(result).containsExactly(6 to 11)
    }

    @Test
    fun `getLineRanges cursor at start of second line`() {
        val text = "Hello\nWorld"
        val result = SpanAdjustmentEngine.getLineRanges(text, 6, 6)
        assertThat(result).containsExactly(6 to 11)
    }

    @Test
    fun `getLineRanges trailing newline creates empty line`() {
        val text = "Hello\n"
        // Cursor at position 6 (after the newline, on empty trailing line)
        val result = SpanAdjustmentEngine.getLineRanges(text, 6, 6)
        assertThat(result).containsExactly(6 to 6)
    }

    @Test
    fun `getLineRanges selection spanning multiple lines`() {
        val text = "Line1\nLine2\nLine3"
        // Selection from position 3 (in Line1) to position 9 (in Line2)
        val result = SpanAdjustmentEngine.getLineRanges(text, 3, 9)
        assertThat(result).hasSize(2)
        assertThat(result[0]).isEqualTo(0 to 6)
        assertThat(result[1]).isEqualTo(6 to 12)
    }

    @Test
    fun `getLineRanges cursor at end of text without trailing newline`() {
        val text = "Hello"
        val result = SpanAdjustmentEngine.getLineRanges(text, 5, 5)
        assertThat(result).containsExactly(0 to 5)
    }

    // ── addSpanToRange / removeSpanFromRange ────────────────────────────

    @Test
    fun `addSpanToRange merges adjacent spans of same type`() {
        val spans = listOf(RichSpan(SpanType.BOLD, 0, 3))
        val result = SpanAdjustmentEngine.addSpanToRange(spans, SpanType.BOLD, 3, 6)
        assertThat(result).containsExactly(RichSpan(SpanType.BOLD, 0, 6))
    }

    @Test
    fun `addSpanToRange merges overlapping spans`() {
        val spans = listOf(RichSpan(SpanType.BOLD, 0, 5))
        val result = SpanAdjustmentEngine.addSpanToRange(spans, SpanType.BOLD, 3, 8)
        assertThat(result).containsExactly(RichSpan(SpanType.BOLD, 0, 8))
    }

    @Test
    fun `addSpanToRange does not merge different types`() {
        val spans = listOf(RichSpan(SpanType.ITALIC, 0, 5))
        val result = SpanAdjustmentEngine.addSpanToRange(spans, SpanType.BOLD, 0, 5)
        assertThat(result).hasSize(2)
    }

    @Test
    fun `removeSpanFromRange splits span around removed range`() {
        val spans = listOf(RichSpan(SpanType.BOLD, 0, 10))
        val result = SpanAdjustmentEngine.removeSpanFromRange(spans, SpanType.BOLD, 3, 7)
        assertThat(result).containsExactly(
            RichSpan(SpanType.BOLD, 0, 3),
            RichSpan(SpanType.BOLD, 7, 10)
        )
    }

    @Test
    fun `removeSpanFromRange removes span entirely when range covers it`() {
        val spans = listOf(RichSpan(SpanType.BOLD, 2, 5))
        val result = SpanAdjustmentEngine.removeSpanFromRange(spans, SpanType.BOLD, 0, 10)
        assertThat(result).isEmpty()
    }

    @Test
    fun `removeSpanFromRange does not affect different types`() {
        val spans = listOf(RichSpan(SpanType.ITALIC, 0, 5))
        val result = SpanAdjustmentEngine.removeSpanFromRange(spans, SpanType.BOLD, 0, 5)
        assertThat(result).containsExactly(RichSpan(SpanType.ITALIC, 0, 5))
    }
}
