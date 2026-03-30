package com.mintanable.notethepad.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.mintanable.notethepad.core.model.settings.NoteShape
import kotlin.random.Random

fun DrawScope.drawNoteShape(noteShape: NoteShape, noteColorInt: Int) {
    val noteColor = Color(noteColorInt)
    when (noteShape) {
        NoteShape.DEFAULT -> defaultNote(noteColor)
        NoteShape.STICKY_CLIPPED -> drawStickyNote(noteColor)
        NoteShape.PERFORATED_PAPER -> drawPerforatedPaper(noteColor)
        NoteShape.FROSTED_GLASS -> drawFrostedGlass(noteColor)
        NoteShape.TAPED_NOTE -> drawTapedNote(noteColor)
        NoteShape.BLUEPRINT_GRID -> drawBlueprintGrid(noteColor)
        NoteShape.SCALLOPED_EDGE -> drawScallopedEdge(noteColor)
        NoteShape.STICKY_CORNER_CURL -> drawStickyCornerCurl(noteColor)
        NoteShape.CRUSHED_PAPER -> drawCrushedPaper(noteColor)
        NoteShape.WASHI_TAPE_TOP -> drawWashiTapeTop(noteColor)
        NoteShape.STAPLED_CORNER -> drawStapledCorner(noteColor)
        NoteShape.PINNED_NOTE -> drawPinnedNote(noteColor)
        NoteShape.SUN_BLEACHED_FADE -> drawSunBleachedFade(noteColor)
    }
}

private fun DrawScope.drawSunlitGradient(noteColor: Color, noteColorInt: Int) {
    val corner = 12.dp.toPx()
    drawRoundRect(
        brush = Brush.verticalGradient(
            0.0f to noteColor.copy(alpha = 0.9f),
            0.7f to ColorUtils.blendARGB(noteColorInt, 0xFFFFFF, 0.7f).let { Color(it) },
            1.0f to Color.White.copy(alpha = 0.9f)
        ),
        size = size,
        cornerRadius = CornerRadius(corner)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.1f),
        size = size,
        cornerRadius = CornerRadius(corner),
        style = Stroke(width = 1.dp.toPx())
    )
}

private fun DrawScope.drawPerforatedPaper(noteColor: Color) {
    val holeRadius = 4.dp.toPx()
    val holeSpacing = 12.dp.toPx()
    drawRoundRect(color = noteColor, size = size, cornerRadius = CornerRadius(8.dp.toPx()))
    val totalHoles = (size.width / (holeSpacing + holeRadius * 2)).toInt()
    for (i in 0 until totalHoles) {
        drawCircle(
            color = Color.Black.copy(alpha = 0.2f),
            radius = holeRadius,
            center = Offset((i * (holeSpacing + holeRadius * 2)) + holeSpacing, 12.dp.toPx()),
            blendMode = BlendMode.DstOut
        )
    }
}

private fun DrawScope.drawFrostedGlass(noteColor: Color) {
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(noteColor.copy(alpha = 0.7f), noteColor.copy(alpha = 0.3f))
        ),
        size = size,
        cornerRadius = CornerRadius(16.dp.toPx())
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.2f),
        size = size,
        cornerRadius = CornerRadius(16.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )
}

private fun DrawScope.drawTapedNote(noteColor: Color) {
    drawRect(color = noteColor)
    val tapeWidth = size.width * 0.4f
    val tapeHeight = 24.dp.toPx()
    val startX = (size.width - tapeWidth) / 2
    val startY = -10.dp.toPx()

    val random = Random(noteColor.toArgb().toLong())
    val jagCount = 15
    val maxJag = 4.dp.toPx()

    val tornPath = Path().apply {
        moveTo(startX, startY)
        for (i in 1..jagCount) {
            lineTo(startX + (i.toFloat() / jagCount) * tapeWidth, startY + random.nextFloat() * maxJag)
        }
        lineTo(startX + tapeWidth, startY + tapeHeight)
        for (i in jagCount downTo 0) {
            lineTo(startX + (i.toFloat() / jagCount) * tapeWidth, startY + tapeHeight + random.nextFloat() * maxJag)
        }
        close()
    }
    drawPath(path = tornPath, color = Color.White.copy(alpha = 0.5f))
}

private fun DrawScope.drawBlueprintGrid(noteColor: Color) {
    drawRect(color = noteColor)
    val step = 20.dp.toPx()
    val gridColor = Color.Black.copy(alpha = 0.2f)
    for (x in 0..(size.width / step).toInt()) {
        drawLine(gridColor, Offset(x * step, 0f), Offset(x * step, size.height))
    }
    for (y in 0..(size.height / step).toInt()) {
        drawLine(gridColor, Offset(0f, y * step), Offset(size.width, y * step))
    }
}

private fun DrawScope.drawScallopedEdge(noteColor: Color) {
    val path = Path().apply {
        val scallop = 8.dp.toPx()
        moveTo(0f, 0f)
        for (i in 0 until (size.width / (scallop * 2)).toInt()) {
            relativeQuadraticTo(scallop, scallop, scallop * 2, 0f)
        }
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(path, color = noteColor)
}

private fun DrawScope.defaultNote(noteColor: Color) {
    drawRoundRect(color = noteColor, cornerRadius = CornerRadius(12.dp.toPx()))
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(Color.Black.copy(0.15f), Color.Transparent),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        ),
        cornerRadius = CornerRadius(12.dp.toPx())
    )
}

private fun DrawScope.drawStickyCornerCurl(noteColor: Color) {
    val cSize = 16.dp.toPx()
    drawRoundRect(color = noteColor, cornerRadius = CornerRadius(8.dp.toPx()))
    val curlPath = Path().apply {
        moveTo(size.width - cSize, size.height)
        quadraticTo(size.width - 10.dp.toPx(), size.height - 10.dp.toPx(), size.width, size.height - cSize)
        lineTo(size.width - cSize, size.height - cSize)
        close()
    }
    drawPath(curlPath, color = Color.White.copy(alpha = 0.4f))
}

private fun DrawScope.drawCrushedPaper(noteColor: Color) {
    drawRoundRect(color = noteColor, cornerRadius = CornerRadius(12.dp.toPx()))
    val random = Random(noteColor.toArgb().toLong())
    val count = (size.width * size.height / 200).toInt().coerceAtMost(300)
    repeat(count) { i ->
        drawCircle(
            color = if (i % 2 == 0) Color.White.copy(0.1f) else Color.Black.copy(0.1f),
            radius = 1.5.dp.toPx(),
            center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
        )
    }
}

private fun DrawScope.drawWashiTapeTop(noteColor: Color) {
    drawRect(color = noteColor)
    val random = Random(noteColor.toArgb().toLong())
    val tapePath = Path().apply {
        moveTo(0f, 0f)
        for (i in 1..15) {
            lineTo(i * (size.width / 15), if (i % 2 == 0) 0f else (random.nextFloat() * 5.dp.toPx()))
        }
        lineTo(size.width, 24.dp.toPx())
        lineTo(0f, 24.dp.toPx())
        close()
    }
    drawPath(tapePath, color = Color.White.copy(alpha = 0.5f))
}

private fun DrawScope.drawStapledCorner(noteColor: Color) {
    val stapleWidth = 24.dp.toPx()
    val stapleHeight = 6.dp.toPx()
    val margin = 16.dp.toPx()

    drawRoundRect(color = noteColor, cornerRadius = CornerRadius(8.dp.toPx()))

    // Top Right Staple
    drawRoundRect(
        color = Color(0xFF606060),
        topLeft = Offset(x = size.width - stapleWidth - margin, y = margin),
        size = Size(stapleWidth, stapleHeight),
        cornerRadius = CornerRadius(2.dp.toPx())
    )
}

private fun DrawScope.drawPinnedNote(noteColor: Color) {
    val pinRadius = 8.dp.toPx()
    val pinY = 0f

    val random = Random(noteColor.toArgb().toLong())

   val minX = size.width * 0.125f
    val maxX = size.width * 0.875f
    val centerX = minX + (random.nextFloat() * (maxX - minX))

    drawRoundRect(
        color = noteColor,
        cornerRadius = CornerRadius(8.dp.toPx())
    )

    drawCircle(
        color = Color.Black.copy(alpha = 0.15f),
        radius = pinRadius + 1.dp.toPx(),
        center = Offset(centerX + 2.dp.toPx(), pinY + 4.dp.toPx())
    )

    drawCircle(
        color = Color.Red,
        radius = pinRadius,
        center = Offset(centerX, pinY)
    )

    drawCircle(
        color = Color.White.copy(0.4f),
        radius = 3.dp.toPx(),
        center = Offset(centerX - 2.dp.toPx(), pinY - 2.dp.toPx())
    )
}

private fun DrawScope.drawSunBleachedFade(noteColor: Color) {
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(noteColor, Color.White.copy(0.8f))),
        cornerRadius = CornerRadius(12.dp.toPx())
    )
}

private fun DrawScope.drawStickyNote(noteColor: Color) {
    val cutCornerSize = 30.dp
    val cornerRadius = 10.dp
    val clipPath = Path().apply {
        lineTo(size.width - cutCornerSize.toPx(), 0f)
        lineTo(size.width, cutCornerSize.toPx())
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    clipPath(clipPath) {
        drawRoundRect(color = noteColor, size = size, cornerRadius = CornerRadius(cornerRadius.toPx()))
        drawRoundRect(
            color = Color(ColorUtils.blendARGB(noteColor.toArgb(), 0x000000, 0.2f)),
            topLeft = Offset(size.width - cutCornerSize.toPx(), -100f),
            size = Size(cutCornerSize.toPx() + 100f, cutCornerSize.toPx() + 100f),
            cornerRadius = CornerRadius(cornerRadius.toPx())
        )
    }
}