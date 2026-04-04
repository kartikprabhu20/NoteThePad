package com.mintanable.notethepad.shapes

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

class ReverseTrapezoidShape(private val slantOffset: Float = 40f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            // Point 1: Top Left
            moveTo(x = 0f, y = 0f)
            // Point 2: Top Right (inset by slant)
            lineTo(x = size.width, y = 0f)
            // Point 3: Bottom Right (inset by slant)
            lineTo(x = size.width - slantOffset, y = size.height)
            // Point 4: Bottom Left (inset by slant)
            lineTo(x = slantOffset, y = size.height)
            close()
        }
        return Outline.Generic(path)
    }
}