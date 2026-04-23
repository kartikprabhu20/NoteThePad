package com.mintanable.notethepad.feature_note.presentation.paint

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews
import kotlinx.coroutines.delay

@Composable
fun PaintBar(
    activeTool: PaintTool,
    onToolClick: (PaintTool) -> Unit,
    modifier: Modifier = Modifier
) {

    val wiggleAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(200)
        wiggleAnim.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 500
                0f at 0
                -20f at 100
                20f at 200
                -10f at 300
                10f at 400
                0f at 500
            }
        )
    }

    val wiggleModifier = Modifier.graphicsLayer { rotationZ = wiggleAnim.value }


    BottomAppBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        contentPadding = PaddingValues(0.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PaintToolButton(
                painter = painterResource(R.drawable.border_color_24),
                contentDescription = stringResource(R.string.content_description_paint_brush),
                isSelected = activeTool == PaintTool.BRUSH,
                onClick = { onToolClick(PaintTool.BRUSH) },
                modifier = wiggleModifier
            )
            PaintToolButton(
                painter = painterResource(R.drawable.format_ink_highlighter_24),
                contentDescription = stringResource(R.string.content_description_highlitger),
                isSelected = activeTool == PaintTool.HIGHLIGHTER,
                onClick = { onToolClick(PaintTool.HIGHLIGHTER) },
                modifier = wiggleModifier
            )
            PaintToolButton(
                painter = painterResource(R.drawable.ink_eraser_24),
                contentDescription = stringResource(R.string.content_description_paint_eraser),
                isSelected = activeTool == PaintTool.ERASER,
                onClick = { onToolClick(PaintTool.ERASER) },
                modifier = wiggleModifier
            )
        }
    }
}

@Composable
private fun PaintToolButton(
    icon: ImageVector? = null,
    painter: Painter? = null,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SmallFloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        containerColor = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.size(40.dp)
    ) {

        if(icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if(painter != null) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@ThemePreviews
@Composable
fun PaintBarPreview() {
    NoteThePadTheme {
        PaintBar(
            activeTool = PaintTool.BRUSH,
            onToolClick = {}
        )
    }
}
