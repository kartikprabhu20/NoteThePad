package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.theme.NoteThePadTheme

val BAR_SPACE: Int =  2

@Composable
fun AmplitudeBarGraph(
    amplitudeLevels: List<Float>,
    progress: Float,
    modifier: Modifier = Modifier,
    barColor: Color = ProgressIndicatorDefaults.linearColor,
    progressColor: Color = ProgressIndicatorDefaults.linearTrackColor
) {
    Canvas(modifier = modifier) {
        val barCount = amplitudeLevels.size
        val barWidth = (size.width - 2.dp.toPx() * (barCount - 1)) / barCount
        val cornerRadius = CornerRadius(x = barWidth, y = barWidth)

        // Use drawIntoCanvas for advanced blend mode operations
        drawIntoCanvas { canvas ->

            canvas.saveLayer(size.toRect(), androidx.compose.ui.graphics.Paint())

            amplitudeLevels.forEachIndexed { index, level ->
                val barHeight = (level * size.height).coerceAtLeast(1.5f)
                val left = index * (barWidth + BAR_SPACE.dp.toPx())
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x = left, y = size.height / 2 - barHeight / 2),
                    size = Size(barWidth, barHeight),
                    cornerRadius = cornerRadius,
                )
            }

            val progressWidth = size.width * progress
            clipRect(right = progressWidth) { // This "cuts" the drawing area
                amplitudeLevels.forEachIndexed { index, level ->
                    val barHeight = (level * size.height).coerceAtLeast(1.5f)
                    val left = index * (barWidth + BAR_SPACE.dp.toPx())
                    drawRoundRect(
                        color = progressColor, // 1.0f Alpha
                        topLeft = Offset(x = left, y = size.height / 2 - barHeight / 2),
                        size = Size(barWidth, barHeight),
                        cornerRadius = cornerRadius
                    )
                }
            }

            canvas.restore()
        }
    }
}