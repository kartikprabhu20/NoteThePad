package com.mintanable.notethepad.core.richtext.serializer

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
}
