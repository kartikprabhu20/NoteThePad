package com.mintanable.notethepad.core.richtext.compose

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.mintanable.notethepad.core.richtext.model.RichTextDocument
import com.mintanable.notethepad.core.richtext.model.SpanType

object RichTextAnnotator {

    fun toAnnotatedString(doc: RichTextDocument): AnnotatedString {
        return buildAnnotatedString {
            append(doc.rawText)
            doc.spans.forEach { span ->
                val style = spanStyleFor(span.type) ?: return@forEach
                addStyle(style, span.start, minOf(span.end, doc.rawText.length))
            }
        }
    }

    private fun spanStyleFor(type: SpanType): SpanStyle? = when (type) {
        SpanType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
        SpanType.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
        SpanType.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
        SpanType.H1 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp)
        SpanType.H2 -> SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        SpanType.PARAGRAPH -> null
        SpanType.BULLET -> null // bullet prefix rendered by UI layer via annotation
    }
}
