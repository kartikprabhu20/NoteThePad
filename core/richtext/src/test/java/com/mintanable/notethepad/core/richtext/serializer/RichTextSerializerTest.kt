package com.mintanable.notethepad.core.richtext.serializer

import com.google.common.truth.Truth.assertThat
import com.mintanable.notethepad.core.richtext.model.RichSpan
import com.mintanable.notethepad.core.richtext.model.RichTextDocument
import com.mintanable.notethepad.core.richtext.model.SpanType
import org.junit.Test

class RichTextSerializerTest {

    @Test
    fun `serialize and deserialize roundtrip preserves document`() {
        val doc = RichTextDocument.of("Hello World", listOf(
            RichSpan(SpanType.BOLD, 0, 5),
            RichSpan(SpanType.ITALIC, 6, 11)
        ))
        val json = RichTextSerializer.serialize(doc)
        val restored = RichTextSerializer.deserialize(json)

        assertThat(restored.rawText).isEqualTo(doc.rawText)
        assertThat(restored.spans).hasSize(2)
        assertThat(restored.spans).containsExactlyElementsIn(doc.spans)
    }

    @Test
    fun `serialize produces valid JSON with rawText and spans`() {
        val doc = RichTextDocument.of("Hi", listOf(RichSpan(SpanType.BOLD, 0, 2)))
        val json = RichTextSerializer.serialize(doc)
        assertThat(json).contains("\"rawText\"")
        assertThat(json).contains("\"spans\"")
        assertThat(json).contains("\"bold\"")
    }

    @Test
    fun `deserialize empty string returns EMPTY`() {
        val result = RichTextSerializer.deserialize("")
        assertThat(result).isEqualTo(RichTextDocument.EMPTY)
    }

    @Test
    fun `deserialize blank string returns EMPTY`() {
        val result = RichTextSerializer.deserialize("   ")
        assertThat(result).isEqualTo(RichTextDocument.EMPTY)
    }

    @Test
    fun `deserialize plain text string returns document with no spans`() {
        val result = RichTextSerializer.deserialize("Just plain text")
        assertThat(result.rawText).isEqualTo("Just plain text")
        assertThat(result.spans).isEmpty()
    }

    @Test
    fun `deserialize malformed JSON returns plain text fallback`() {
        val result = RichTextSerializer.deserialize("{broken json!!!")
        assertThat(result.rawText).isEqualTo("{broken json!!!")
        assertThat(result.spans).isEmpty()
    }

    @Test
    fun `deserialize ignores spans with unknown type`() {
        val json = """{"rawText":"Hello","spans":[{"type":"unknown","start":0,"end":5}]}"""
        val result = RichTextSerializer.deserialize(json)
        assertThat(result.rawText).isEqualTo("Hello")
        assertThat(result.spans).isEmpty()
    }

    @Test
    fun `deserialize ignores spans with out-of-bounds indices`() {
        val json = """{"rawText":"Hi","spans":[{"type":"bold","start":0,"end":100}]}"""
        val result = RichTextSerializer.deserialize(json)
        assertThat(result.rawText).isEqualTo("Hi")
        assertThat(result.spans).isEmpty()
    }

    @Test
    fun `deserialize ignores spans where start equals end`() {
        val json = """{"rawText":"Hello","spans":[{"type":"bold","start":3,"end":3}]}"""
        val result = RichTextSerializer.deserialize(json)
        assertThat(result.spans).isEmpty()
    }

    @Test
    fun `roundtrip with all span types`() {
        val doc = RichTextDocument.of("Hello World Test Line", listOf(
            RichSpan(SpanType.BOLD, 0, 5),
            RichSpan(SpanType.ITALIC, 0, 5),
            RichSpan(SpanType.UNDERLINE, 6, 11),
            RichSpan(SpanType.H1, 0, 21),
            RichSpan(SpanType.BULLET, 0, 21)
        ))
        val restored = RichTextSerializer.deserialize(RichTextSerializer.serialize(doc))
        assertThat(restored.spans).hasSize(5)
        assertThat(restored.rawText).isEqualTo(doc.rawText)
    }

    @Test
    fun `roundtrip with empty spans list`() {
        val doc = RichTextDocument.of("No formatting", emptyList())
        val restored = RichTextSerializer.deserialize(RichTextSerializer.serialize(doc))
        assertThat(restored.rawText).isEqualTo("No formatting")
        assertThat(restored.spans).isEmpty()
    }

    @Test
    fun `roundtrip with empty text`() {
        val doc = RichTextDocument.of("", emptyList())
        val restored = RichTextSerializer.deserialize(RichTextSerializer.serialize(doc))
        assertThat(restored.rawText).isEmpty()
        assertThat(restored.spans).isEmpty()
    }
}
