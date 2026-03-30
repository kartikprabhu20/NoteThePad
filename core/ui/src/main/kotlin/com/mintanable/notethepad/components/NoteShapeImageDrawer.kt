package com.mintanable.notethepad.components

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.core.model.settings.NoteShape
import kotlin.random.Random

fun DrawScope.drawNoteWithImage(
    noteShape: NoteShape,
    noteColorInt: Int,
    imagePainter: Painter?,
    imageAlpha: Float = 0.8f,
    isDarkTheme: Boolean
) {
    val cornerPx = 12.dp.toPx()
    val width = size.width
    val height = size.height
    val basePaperColor = if (noteColorInt == -1) {
        if (isDarkTheme) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)
    } else {
        Color(noteColorInt)
    }

    //Define the clipping path for the "Paper"
    val paperPath = Path().apply {
        when (noteShape) {
            NoteShape.SCALLOPED_EDGE -> {
                val scallop = 8.dp.toPx()
                moveTo(0f, 0f)
                for (i in 0 until (width / (scallop * 2)).toInt()) {
                    relativeQuadraticTo(scallop, scallop, scallop * 2, 0f)
                }
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            NoteShape.STICKY_CLIPPED -> {
                val cutSize = 30.dp.toPx()
                moveTo(0f, 0f)
                lineTo(width - cutSize, 0f)
                lineTo(width, cutSize)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            else -> {
                addRoundRect(RoundRect(0f, 0f, width, height, CornerRadius(cornerPx)))
            }
        }
    }

    //Draw the Background (Color or Image)
    clipPath(paperPath) {
        // Base Color (Always draw this as a fallback/underlay)
        drawPath(path = paperPath, color = basePaperColor)

        // Overlay Image if exists
        if (imagePainter != null && imageAlpha > 0f) {
            with(imagePainter) {
                draw(size = size, alpha = imageAlpha)
            }
            // Subtle darkening so text remains readable
            drawRect(
                color = Color.Black.copy(alpha = if (isDarkTheme) 0.2f else 0.05f),
                blendMode = BlendMode.Darken
            )
        }
    }

    //Draw Specific Shape Decorations/Overlays
    val decorationSeed = basePaperColor.toArgb().toLong()
    when (noteShape) {
        NoteShape.DEFAULT -> {
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(Color.Black.copy(0.1f), Color.Transparent),
                    start = Offset.Zero, end = Offset(width, height)
                ),
                cornerRadius = CornerRadius(cornerPx)
            )
        }
        NoteShape.PERFORATED_PAPER -> {
            val holeRadius = 4.dp.toPx()
            val holeSpacing = 12.dp.toPx()
            val totalHoles = (width / (holeSpacing + holeRadius * 2)).toInt()
            for (i in 0 until totalHoles) {
                drawCircle(
                    color = Color.Transparent,
                    radius = holeRadius,
                    center = Offset((i * (holeSpacing + holeRadius * 2)) + holeSpacing, 12.dp.toPx()),
                    blendMode = BlendMode.Clear // Punches holes through EVERYTHING
                )
            }
        }
        NoteShape.FROSTED_GLASS -> {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.1f),
                style = Stroke(width = 1.dp.toPx()),
                cornerRadius = CornerRadius(16.dp.toPx())
            )
        }
        NoteShape.TAPED_NOTE -> {
            val tapeWidth = width * 0.4f
            val tapeHeight = 24.dp.toPx()
            val startX = (width - tapeWidth) / 2
            val random = Random(decorationSeed)
            val tapePath = Path().apply {
                moveTo(startX, -5.dp.toPx())
                repeat(10) { i ->
                    lineTo(startX + (i/10f)*tapeWidth, -5.dp.toPx() + random.nextFloat()*4.dp.toPx())
                }
                lineTo(startX + tapeWidth, tapeHeight)
                lineTo(startX, tapeHeight)
                close()
            }
            drawPath(tapePath, Color.White.copy(0.4f))
        }
        NoteShape.BLUEPRINT_GRID -> {
            val step = 20.dp.toPx()
            val gridColor = if (isDarkTheme) Color.White.copy(0.05f) else Color.Black.copy(0.05f)
            for (x in 0..(width / step).toInt()) {
                drawLine(gridColor, Offset(x * step, 0f), Offset(x * step, height))
            }
            for (y in 0..(height / step).toInt()) {
                drawLine(gridColor, Offset(0f, y * step), Offset(width, y * step))
            }
        }
        NoteShape.STICKY_CORNER_CURL -> {
            val cSize = 20.dp.toPx()
            val curlPath = Path().apply {
                moveTo(width - cSize, height)
                quadraticTo(width - 8.dp.toPx(), height - 8.dp.toPx(), width, height - cSize)
                lineTo(width - cSize, height - cSize)
                close()
            }
            drawPath(curlPath, Color.White.copy(0.3f))
        }
        NoteShape.CRUSHED_PAPER -> {
            val random = Random(decorationSeed)
            repeat(100) {
                drawCircle(
                    color = Color.Black.copy(0.03f),
                    radius = 1.dp.toPx(),
                    center = Offset(random.nextFloat() * width, random.nextFloat() * height)
                )
            }
        }
        NoteShape.WASHI_TAPE_TOP -> {
            drawRect(
                color = Color.White.copy(0.3f),
                size = Size(width, 20.dp.toPx()),
                topLeft = Offset(0f, 0f)
            )
        }
        NoteShape.STAPLED_CORNER -> {
            drawRoundRect(
                color = Color(0xFF808080),
                topLeft = Offset(width - 40.dp.toPx(), 16.dp.toPx()),
                size = Size(24.dp.toPx(), 6.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
        NoteShape.PINNED_NOTE -> {
            val centerX = width / 2f
            drawCircle(Color.Black.copy(0.2f), 9.dp.toPx(), Offset(centerX + 2.dp.toPx(), 4.dp.toPx()))
            drawCircle(Color.Red, 8.dp.toPx(), Offset(centerX, 0f))
            drawCircle(Color.White.copy(0.4f), 3.dp.toPx(), Offset(centerX - 2.dp.toPx(), -2.dp.toPx()))
        }
        NoteShape.SUN_BLEACHED_FADE -> {
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(0.3f))),
                size = size
            )
        }

        else -> {}
    }
}