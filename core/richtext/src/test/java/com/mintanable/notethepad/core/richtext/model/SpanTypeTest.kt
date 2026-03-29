package com.mintanable.notethepad.core.richtext.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpanTypeTest {

    @Test
    fun `fromJsonKey returns correct type for valid keys`() {
        assertThat(SpanType.fromJsonKey("bold")).isEqualTo(SpanType.BOLD)
        assertThat(SpanType.fromJsonKey("italic")).isEqualTo(SpanType.ITALIC)
        assertThat(SpanType.fromJsonKey("underline")).isEqualTo(SpanType.UNDERLINE)
        assertThat(SpanType.fromJsonKey("h1")).isEqualTo(SpanType.H1)
        assertThat(SpanType.fromJsonKey("h2")).isEqualTo(SpanType.H2)
        assertThat(SpanType.fromJsonKey("paragraph")).isEqualTo(SpanType.PARAGRAPH)
        assertThat(SpanType.fromJsonKey("bullet")).isEqualTo(SpanType.BULLET)
    }

    @Test
    fun `fromJsonKey returns null for unknown key`() {
        assertThat(SpanType.fromJsonKey("unknown")).isNull()
        assertThat(SpanType.fromJsonKey("")).isNull()
    }

    @Test
    fun `jsonKey roundtrips correctly for all types`() {
        SpanType.entries.forEach { type ->
            assertThat(SpanType.fromJsonKey(type.jsonKey)).isEqualTo(type)
        }
    }
}
