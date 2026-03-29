package com.mintanable.notethepad.core.richtext.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RichTextDocumentTest {

    @Test
    fun `of filters out empty spans`() {
        val spans = listOf(
            RichSpan(SpanType.BOLD, 0, 5),
            RichSpan(SpanType.ITALIC, 3, 3) // empty
        )
        val doc = RichTextDocument.of("Hello", spans)
        assertThat(doc.spans).hasSize(1)
        assertThat(doc.spans[0].type).isEqualTo(SpanType.BOLD)
    }

    @Test
    fun `of sorts spans by start then type ordinal`() {
        val spans = listOf(
            RichSpan(SpanType.ITALIC, 2, 5),
            RichSpan(SpanType.BOLD, 0, 3),
            RichSpan(SpanType.BOLD, 2, 7)
        )
        val doc = RichTextDocument.of("Hello World", spans)
        assertThat(doc.spans[0]).isEqualTo(RichSpan(SpanType.BOLD, 0, 3))
        // Same start=2: BOLD (ordinal 0) before ITALIC (ordinal 1)
        assertThat(doc.spans[1]).isEqualTo(RichSpan(SpanType.BOLD, 2, 7))
        assertThat(doc.spans[2]).isEqualTo(RichSpan(SpanType.ITALIC, 2, 5))
    }

    @Test
    fun `isActiveAt returns true when span fully covers range`() {
        val doc = RichTextDocument.of("Hello", listOf(RichSpan(SpanType.BOLD, 0, 5)))
        assertThat(doc.isActiveAt(SpanType.BOLD, 0, 5)).isTrue()
        assertThat(doc.isActiveAt(SpanType.BOLD, 1, 3)).isTrue()
    }

    @Test
    fun `isActiveAt returns false when span partially covers range`() {
        val doc = RichTextDocument.of("Hello", listOf(RichSpan(SpanType.BOLD, 0, 3)))
        assertThat(doc.isActiveAt(SpanType.BOLD, 0, 5)).isFalse()
    }

    @Test
    fun `isActiveAt returns false for wrong type`() {
        val doc = RichTextDocument.of("Hello", listOf(RichSpan(SpanType.BOLD, 0, 5)))
        assertThat(doc.isActiveAt(SpanType.ITALIC, 0, 5)).isFalse()
    }

    @Test
    fun `activeTypesAt returns all types active at offset`() {
        val doc = RichTextDocument.of("Hello", listOf(
            RichSpan(SpanType.BOLD, 0, 5),
            RichSpan(SpanType.ITALIC, 2, 5)
        ))
        assertThat(doc.activeTypesAt(3)).containsExactly(SpanType.BOLD, SpanType.ITALIC)
        assertThat(doc.activeTypesAt(0)).containsExactly(SpanType.BOLD)
    }

    @Test
    fun `activeTypesAt returns empty set for offset outside all spans`() {
        val doc = RichTextDocument.of("Hello World", listOf(
            RichSpan(SpanType.BOLD, 0, 5)
        ))
        assertThat(doc.activeTypesAt(6)).isEmpty()
    }

    @Test
    fun `EMPTY document has no text and no spans`() {
        assertThat(RichTextDocument.EMPTY.rawText).isEmpty()
        assertThat(RichTextDocument.EMPTY.spans).isEmpty()
    }
}
