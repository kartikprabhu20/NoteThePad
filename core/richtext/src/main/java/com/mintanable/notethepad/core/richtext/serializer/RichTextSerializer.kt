package com.mintanable.notethepad.core.richtext.serializer

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mintanable.notethepad.core.richtext.model.RichSpan
import com.mintanable.notethepad.core.richtext.model.RichTextDocument
import com.mintanable.notethepad.core.richtext.model.SpanType

/**
 * Serializes and deserializes [RichTextDocument] to/from JSON.
 *
 * Format: { "rawText": "Hello", "spans": [{ "type": "bold", "start": 0, "end": 5 }] }
 *
 * Any string that is not valid JSON in this shape is treated as plain text with no spans
 * (backward compatibility with existing notes).
 */
object RichTextSerializer {

    fun serialize(doc: RichTextDocument): String {
        val root = JsonObject()
        root.addProperty("rawText", doc.rawText)
        val spansArray = JsonArray()
        doc.spans.forEach { span ->
            val obj = JsonObject()
            obj.addProperty("type", span.type.jsonKey)
            obj.addProperty("start", span.start)
            obj.addProperty("end", span.end)
            spansArray.add(obj)
        }
        root.add("spans", spansArray)
        return root.toString()
    }

    fun deserialize(stored: String): RichTextDocument {
        if (stored.isBlank()) return RichTextDocument.EMPTY
        return runCatching {
            val root = JsonParser.parseString(stored).asJsonObject
            val rawText = root.get("rawText")?.asString ?: return plainText(stored)
            val spansArray = root.getAsJsonArray("spans") ?: JsonArray()
            val spans = mutableListOf<RichSpan>()
            spansArray.forEach { element ->
                val obj = element.asJsonObject
                val typeKey = obj.get("type")?.asString ?: return@forEach
                val spanType = SpanType.fromJsonKey(typeKey) ?: return@forEach
                val start = obj.get("start")?.asInt ?: return@forEach
                val end = obj.get("end")?.asInt ?: return@forEach
                if (end > start && start >= 0 && end <= rawText.length) {
                    spans.add(RichSpan(spanType, start, end))
                }
            }
            RichTextDocument.of(rawText, spans)
        }.getOrElse { plainText(stored) }
    }

    private fun plainText(raw: String) = RichTextDocument(rawText = raw, spans = emptyList())

    fun toSpannable(doc: RichTextDocument) : SpannableString {
        val spannable = SpannableString(doc.rawText)

        doc.spans.forEach { span ->
            val start = span.start.coerceIn(0, doc.rawText.length)
            val end = span.end.coerceIn(start, doc.rawText.length)

            when (span.type) {
                SpanType.BOLD -> {
                    spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                SpanType.ITALIC -> {
                    spannable.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                SpanType.UNDERLINE -> {
                    spannable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                SpanType.H1 -> {
                    // H1 is Bold + 1.5x larger
                    spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannable.setSpan(RelativeSizeSpan(1.5f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                SpanType.H2 -> {
                    // H2 is Bold + 1.2x larger
                    spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannable.setSpan(RelativeSizeSpan(1.2f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                SpanType.BULLET -> {
                    // For widgets, bullets are best handled by the deserializer
                    // prepending "• " to the rawText, as BulletSpan is buggy in RemoteViews.
                }
                else -> {}
            }
        }
        return spannable
    }
}
