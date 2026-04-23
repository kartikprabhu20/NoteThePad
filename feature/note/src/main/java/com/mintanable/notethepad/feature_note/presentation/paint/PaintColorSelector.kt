package com.mintanable.notethepad.feature_note.presentation.paint

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews

val BrushSizes: List<Dp> = listOf(2.dp, 4.dp, 8.dp, 16.dp, 24.dp, 32.dp)

@Composable
fun ColorSelector(
    selectedColor: Int,
    onColorClick: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NoteColors.colorPairs.forEach { colorPair ->
                            val lightArgb = colorPair.light.toArgb()
                            ColorCircle(
                                color = colorPair.light,
                                isSelected = selectedColor == lightArgb,
                                onClick = { onColorClick(lightArgb) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NoteColors.colorPairs.forEach { colorPair ->
                            val darkArgb = colorPair.dark.toArgb()
                            ColorCircle(
                                color = colorPair.dark,
                                isSelected = selectedColor == darkArgb,
                                onClick = { onColorClick(darkArgb) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderSize by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 1.dp,
        label = "selection_border_anim"
    )

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(color)
            .border(
                width = borderSize,
                color = borderColor,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PaintBrushOptionsSheetContent(
    selectedColor: Int,
    onColorClick: (Int) -> Unit,
    selectedSizeDp: Dp,
    onSizeClick: (Dp) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrushSizes.forEach { size ->
                BrushSizeDot(
                    sizeDp = size,
                    isSelected = size == selectedSizeDp,
                    onClick = { onSizeClick(size) },
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        ColorSelector(
            selectedColor = selectedColor,
            onColorClick = onColorClick,
        )
    }
}

@Composable
private fun BrushSizeDot(
    sizeDp: Dp,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderSize by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        label = "brush_size_border_anim"
    )
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(width = borderSize, color = borderColor, shape = CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface)
        )
    }
}

@Composable
fun PaintEraserOptionsSheetContent(
    selectedSizeDp: Dp,
    onSizeClick: (Dp) -> Unit,
    onClearClick: () -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrushSizes.forEach { size ->
                BrushSizeDot(
                    sizeDp = size,
                    isSelected = size == selectedSizeDp,
                    onClick = { onSizeClick(size) },
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClearClick() }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.paint_clear_canvas),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@ThemePreviews
@Composable
fun ColorSelectorPreview() {
    val isDark = isSystemInDarkTheme()

    NoteThePadTheme(darkTheme = isDark) {
        ColorSelector(
            selectedColor = NoteColors.colors[1].toArgb(),
            onColorClick = {},
        )
    }
}

@ThemePreviews
@Composable
fun PaintBrushOptionsSheetContentPreview() {
    val isDark = isSystemInDarkTheme()

    NoteThePadTheme(darkTheme = isDark) {
        PaintBrushOptionsSheetContent(
            selectedColor = NoteColors.colors[1].toArgb(),
            onColorClick = {},
            selectedSizeDp = 8.dp,
            onSizeClick = {},
        )
    }
}

@ThemePreviews
@Composable
fun PaintEraserOptionsSheetContentPreview() {
    val isDark = isSystemInDarkTheme()

    NoteThePadTheme(darkTheme = isDark) {
        PaintEraserOptionsSheetContent(
            selectedSizeDp = 8.dp,
            onSizeClick = {},
            onClearClick = {})
    }
}
