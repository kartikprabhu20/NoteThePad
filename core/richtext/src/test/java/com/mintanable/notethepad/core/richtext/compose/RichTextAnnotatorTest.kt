package com.mintanable.notethepad.core.richtext.compose

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.google.common.truth.Truth.assertThat
import com.mintanable.notethepad.core.richtext.model.RichSpan
import com.mintanable.notethepad.core.richtext.model.RichTextDocument
import com.mintanable.notethepad.core.richtext.model.SpanType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RichTextAnnotatorTest {

    @Test
    fun `toAnnotatedString preserves raw text`() {
        val doc = RichTextDocument.of("Hello World", listOf(RichSpan(SpanType.BOLD, 0, 5)))
        val result = RichTextAnnotator.toAnnotatedString(doc)
        assertThat(result.text).isEqualTo("Hello World")
    }

    @Test
    fun `toAnnotatedString applies bold span style`() {
        val doc = RichTextDocument.of("Hello", listOf(RichSpan(SpanType.BOLD, 0, 5)))
        val result = RichTextAnnotator.toAnnotatedString(doc)
        val styles = result.spanStyles
        assertThat(styles).hasSize(1)
        assertThat(styles[0].item.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(styles[0].start).isEqualTo(0)
        assertThat(styles[0].end).isEqualTo(5)
    }

    @Test
    fun `toAnnotatedString applies italic span style`() {
        val doc = RichTextDocument.of("Hello", listOf(RichSpan(SpanType.ITALIC, 0, 5)))
        val result = RichTextAnnotator.toAnnotatedString(doc)
        assertThat(result.spanStyles[0].item.fontStyle).isEqualTo(FontStyle.Italic)
    }

    @Test
    fun `toAnnotatedString applies underline span style`() {
        val doc = RichTextDocument.of("Hello", listOf(RichSpan(SpanType.UNDERLINE, 0, 5)))
        val result = RichTextAnnotator.toAnnotatedString(doc)
        assertThat(result.spanStyles[0].item.textDecoration).isEqualTo(TextDecoration.Underline)
    }

    @Test
    fun `toAnnotatedString applies H1 with bold and large font`() {
        val doc = RichTextDocument.of("Title", listOf(RichSpan(SpanType.H1, 0, 5)))
        val result = RichTextAnnotator.toAnnotatedString(doc)
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `toAnnotatedString skips PARAGRAPH and BULLET (no visual style)`() {
        val doc = RichTextDocument.of("Hello", listOf(
            RichSpan(SpanType.PARAGRAPH, 0, 5),
            RichSpan(SpanType.BULLET, 0, 5)
        ))
        val result = RichTextAnnotator.toAnnotatedString(doc)
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `toAnnotatedString handles multiple overlapping styles`() {
        val doc = RichTextDocument.of("Hello", listOf(
            RichSpan(SpanType.BOLD, 0, 5),
            RichSpan(SpanType.ITALIC, 2, 5)
        ))
        val result = RichTextAnnotator.toAnnotatedString(doc)
        assertThat(result.spanStyles).hasSize(2)
    }

    @Test
    fun `toAnnotatedString clamps span end to text length`() {
        // Span end exceeds text length — should be clamped
        val doc = RichTextDocument("Hi", listOf(RichSpan(SpanType.BOLD, 0, 100)))
        val result = RichTextAnnotator.toAnnotatedString(doc)
        assertThat(result.spanStyles[0].end).isEqualTo(2)
    }

    @Test
    fun `toAnnotatedString with empty document returns empty string`() {
        val result = RichTextAnnotator.toAnnotatedString(RichTextDocument.EMPTY)
        assertThat(result.text).isEmpty()
        assertThat(result.spanStyles).isEmpty()
    }
}
