package com.mintanable.notethepad.core.richtext.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RichSpanTest {

    @Test
    fun `isEmpty returns true for zero-length span`() {
        val span = RichSpan(SpanType.BOLD, 5, 5)
        assertThat(span.isEmpty()).isTrue()
    }

    @Test
    fun `isEmpty returns false for non-zero span`() {
        val span = RichSpan(SpanType.BOLD, 0, 3)
        assertThat(span.isEmpty()).isFalse()
    }

    @Test
    fun `overlaps detects overlapping spans`() {
        val a = RichSpan(SpanType.BOLD, 0, 5)
        val b = RichSpan(SpanType.BOLD, 3, 8)
        assertThat(a.overlaps(b)).isTrue()
        assertThat(b.overlaps(a)).isTrue()
    }

    @Test
    fun `overlaps returns false for adjacent non-overlapping spans`() {
        val a = RichSpan(SpanType.BOLD, 0, 5)
        val b = RichSpan(SpanType.BOLD, 5, 10)
        assertThat(a.overlaps(b)).isFalse()
        assertThat(b.overlaps(a)).isFalse()
    }

    @Test
    fun `overlaps returns false for disjoint spans`() {
        val a = RichSpan(SpanType.BOLD, 0, 3)
        val b = RichSpan(SpanType.BOLD, 5, 8)
        assertThat(a.overlaps(b)).isFalse()
    }

    @Test
    fun `contains returns true for offset inside span`() {
        val span = RichSpan(SpanType.ITALIC, 2, 7)
        assertThat(span.contains(2)).isTrue()
        assertThat(span.contains(4)).isTrue()
        assertThat(span.contains(6)).isTrue()
    }

    @Test
    fun `contains returns false for offset at end (exclusive)`() {
        val span = RichSpan(SpanType.ITALIC, 2, 7)
        assertThat(span.contains(7)).isFalse()
    }

    @Test
    fun `contains returns false for offset before span`() {
        val span = RichSpan(SpanType.ITALIC, 2, 7)
        assertThat(span.contains(1)).isFalse()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects negative start`() {
        RichSpan(SpanType.BOLD, -1, 5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects end less than start`() {
        RichSpan(SpanType.BOLD, 5, 3)
    }
}
