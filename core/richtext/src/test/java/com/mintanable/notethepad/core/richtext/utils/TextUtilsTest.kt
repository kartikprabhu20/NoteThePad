package com.mintanable.notethepad.core.richtext.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TextUtilsTest {

    // ── findTextChangeStart ─────────────────────────────────────────────

    @Test
    fun `findTextChangeStart returns 0 when first char differs`() {
        assertThat(TextUtils.findTextChangeStart("Hello", "Xello")).isEqualTo(0)
    }

    @Test
    fun `findTextChangeStart returns correct position for mid-string change`() {
        assertThat(TextUtils.findTextChangeStart("Hello", "HeXlo")).isEqualTo(2)
    }

    @Test
    fun `findTextChangeStart returns minLen when one string is prefix of other`() {
        assertThat(TextUtils.findTextChangeStart("Hello", "Hello World")).isEqualTo(5)
    }

    @Test
    fun `findTextChangeStart returns 0 for identical strings`() {
        // Both strings are equal; returns minLen = length of shorter = length of both
        assertThat(TextUtils.findTextChangeStart("same", "same")).isEqualTo(4)
    }

    @Test
    fun `findTextChangeStart handles empty old string (insertion from scratch)`() {
        assertThat(TextUtils.findTextChangeStart("", "Hello")).isEqualTo(0)
    }

    @Test
    fun `findTextChangeStart handles empty new string (full deletion)`() {
        assertThat(TextUtils.findTextChangeStart("Hello", "")).isEqualTo(0)
    }

    // ── findTextChangeEnd ───────────────────────────────────────────────

    @Test
    fun `findTextChangeEnd for single char insertion in middle`() {
        val old = "Helo"
        val new = "Hello"
        val start = TextUtils.findTextChangeStart(old, new)
        val end = TextUtils.findTextChangeEnd(old, new, start)
        // "Helo" vs "Hello": first diff at 3 (o vs l), insertion at position 3
        assertThat(start).isEqualTo(3)
        assertThat(end).isEqualTo(3)
    }

    @Test
    fun `findTextChangeEnd for single char deletion`() {
        val old = "Hello"
        val new = "Helo"
        val start = TextUtils.findTextChangeStart(old, new)
        val end = TextUtils.findTextChangeEnd(old, new, start)
        // "Hello" vs "Helo": first diff at 3 (l vs o), one char deleted
        assertThat(start).isEqualTo(3)
        assertThat(end).isEqualTo(4)
    }

    @Test
    fun `findTextChangeEnd for replacement`() {
        val old = "Hello"
        val new = "HeXXo"
        val start = TextUtils.findTextChangeStart(old, new)
        val end = TextUtils.findTextChangeEnd(old, new, start)
        assertThat(start).isEqualTo(2)
        assertThat(end).isEqualTo(4)
    }

    @Test
    fun `findTextChangeEnd for appending at end`() {
        val old = "Hello"
        val new = "Hello World"
        val start = TextUtils.findTextChangeStart(old, new)
        val end = TextUtils.findTextChangeEnd(old, new, start)
        assertThat(start).isEqualTo(5)
        assertThat(end).isEqualTo(5)
    }

    @Test
    fun `findTextChangeEnd for deleting from end`() {
        val old = "Hello World"
        val new = "Hello"
        val start = TextUtils.findTextChangeStart(old, new)
        val end = TextUtils.findTextChangeEnd(old, new, start)
        assertThat(start).isEqualTo(5)
        assertThat(end).isEqualTo(11)
    }
}
