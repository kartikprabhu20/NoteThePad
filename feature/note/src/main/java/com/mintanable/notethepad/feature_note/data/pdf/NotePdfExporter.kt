package com.mintanable.notethepad.feature_note.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.mintanable.notethepad.core.richtext.serializer.RichTextSerializer
import com.mintanable.notethepad.feature_note.presentation.AddEditNoteUiState
import com.mintanable.notethepad.file.FileManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotePdfExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: FileManager
) {

    suspend fun export(state: AddEditNoteUiState): Uri = withContext(Dispatchers.IO) {
        val pdfDir = File(fileManager.getMediaDir(), "pdf").apply { if (!exists()) mkdirs() }
        val outFile = File(pdfDir, "Note_${System.currentTimeMillis()}.pdf")

        val document = PdfDocument()
        try {
            val renderer = PageRenderer(document)
            renderer.startPage()

            val titleSpanned = RichTextSerializer.toSpannable(state.titleState.richText).trimEndWhitespace()
            if (titleSpanned.isNotEmpty()) {
                renderer.drawBlock(titleSpanned, TitlePaint, spacingAfter = 16f)
            }

            state.attachedImages.forEach { uri ->
                decodeBitmap(uri)?.let { renderer.drawImage(it) }
            }

            state.attachedPaints.forEach { uri ->
                decodeBitmap(uri)?.let { renderer.drawImage(it) }
            }

            if (state.isCheckboxListAvailable) {
                state.checkListItems.forEach { item ->
                    val prefix = if (item.isChecked) "\u2611 " else "\u2610 "
                    renderer.drawBlock(prefix + item.text, BodyPaint, spacingAfter = 6f)
                }
            } else {
                val bodySpanned = RichTextSerializer.toSpannable(state.contentRichTextState.document).trimEndWhitespace()
                if (bodySpanned.isNotEmpty()) {
                    renderer.drawBlock(bodySpanned, BodyPaint, spacingAfter = 12f)
                }
            }

            if (state.tagEntities.isNotEmpty()) {
                val tagsText = state.tagEntities.joinToString(" ") { "#${it.tagName}" }
                renderer.drawBlock(tagsText, TagsPaint, spacingAfter = 0f)
            }

            renderer.finishPage()

            FileOutputStream(outFile).use { document.writeTo(it) }
        } finally {
            document.close()
        }

        FileProvider.getUriForFile(context, fileManager.authority, outFile)
    }

    private fun decodeBitmap(uri: Uri): Bitmap? = try {
        val file = fileManager.getFileFromUri(uri.toString())
        if (file != null && file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
    } catch (_: Exception) {
        null
    }

    private fun CharSequence.trimEndWhitespace(): CharSequence {
        var end = length
        while (end > 0 && this[end - 1].isWhitespace()) end--
        var start = 0
        while (start < end && this[start].isWhitespace()) start++
        return if (start == 0 && end == length) this else subSequence(start, end)
    }

    private inner class PageRenderer(private val document: PdfDocument) {
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var pageNumber = 0
        private var cursorY = 0f

        fun startPage() {
            pageNumber += 1
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val newPage = document.startPage(info)
            page = newPage
            canvas = newPage.canvas
            cursorY = MARGIN
        }

        fun finishPage() {
            page?.let { document.finishPage(it) }
            page = null
            canvas = null
        }

        private fun newPage() {
            finishPage()
            startPage()
        }

        fun drawBlock(text: CharSequence, paint: TextPaint, spacingAfter: Float) {
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, paint, CONTENT_WIDTH)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .build()

            for (i in 0 until layout.lineCount) {
                val lineTop = layout.getLineTop(i).toFloat()
                val lineBottom = layout.getLineBottom(i).toFloat()
                val lineHeight = lineBottom - lineTop

                if (cursorY + lineHeight > MARGIN + CONTENT_HEIGHT) {
                    newPage()
                }
                val c = canvas ?: return
                val saved = c.save()
                c.translate(MARGIN, cursorY - lineTop)
                c.clipRect(0f, lineTop, CONTENT_WIDTH.toFloat(), lineBottom)
                layout.draw(c)
                c.restoreToCount(saved)
                cursorY += lineHeight
            }
            cursorY += spacingAfter
        }

        fun drawImage(bitmap: Bitmap) {
            val scale = CONTENT_WIDTH.toFloat() / bitmap.width.toFloat()
            val targetW = CONTENT_WIDTH
            val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)

            if (cursorY + targetH > MARGIN + CONTENT_HEIGHT) {
                newPage()
                // If a single image is taller than one page, shrink to fit a page.
                if (targetH > CONTENT_HEIGHT) {
                    val scaleToPage = CONTENT_HEIGHT.toFloat() / targetH.toFloat()
                    val fittedH = (targetH * scaleToPage).toInt()
                    val fittedW = (targetW * scaleToPage).toInt()
                    val dst = Rect(
                        MARGIN.toInt(),
                        cursorY.toInt(),
                        MARGIN.toInt() + fittedW,
                        cursorY.toInt() + fittedH
                    )
                    canvas?.drawBitmap(bitmap, null, dst, BitmapPaint)
                    cursorY += fittedH + 8f
                    return
                }
            }

            val dst = Rect(
                MARGIN.toInt(),
                cursorY.toInt(),
                MARGIN.toInt() + targetW,
                cursorY.toInt() + targetH
            )
            canvas?.drawBitmap(bitmap, null, dst, BitmapPaint)
            cursorY += targetH + 8f
        }
    }

    companion object {
        private const val PAGE_WIDTH = 595   // A4 points (1pt = 1/72 inch)
        private const val PAGE_HEIGHT = 842
        private const val MARGIN_INT = 48
        private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN_INT
        private const val CONTENT_HEIGHT = PAGE_HEIGHT - 2 * MARGIN_INT
        private const val MARGIN = MARGIN_INT.toFloat()

        private val TitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = 0xFF111111.toInt()
        }

        private val BodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            typeface = Typeface.DEFAULT
            color = 0xFF222222.toInt()
        }

        private val TagsPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            color = 0xFF666666.toInt()
        }

        private val BitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    }
}
