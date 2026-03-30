package com.mintanable.notethepad.feature_note.presentation.modify.components.sections

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews

@Composable
fun ColorSelectorBottomSheetContent(
    selectedColor: Int,
    selectedBackgroundImage: Int,
    isDarkTheme: Boolean,
    onColorClick: (Int) -> Unit,
    onBackgroundImageClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Row 1: Background Colors
        Text(
            text = stringResource(R.string.subtitle_background_colors),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {

            // 11 color circles
            itemsIndexed(
                items = NoteColors.colorPairs,
                key = { index, _ -> "color_$index" }
            ) { _, colorPair ->
                val displayColor = if (isDarkTheme) colorPair.dark else colorPair.light
                val lightArgb = colorPair.light.toArgb()
                ColorCircle(
                    color = displayColor,
                    isSelected = selectedColor == lightArgb,
                    onClick = { onColorClick(lightArgb) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Row 2: Background Images
        Text(
            text = stringResource(R.string.subtitle_background_images),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            itemsIndexed(
                items = NoteColors.backgroundImages,
                key = { index, _ -> "bg_$index" }
            ) { index, backgroundImage ->

                val backgroundImageRes = if (isDarkTheme) backgroundImage.darkRes else backgroundImage.lightRes

                BackgroundImageCircle(
                    imageRes = backgroundImageRes,
                    isSelected = selectedBackgroundImage == index,
                    onClick = { onBackgroundImageClick(index) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
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
private fun BackgroundImageCircle(
    imageRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderSize by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 1.dp,
        label = "bg_selection_border_anim"
    )

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(borderSize, borderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@ThemePreviews
@Composable
fun ColorSelectorBottomSheetContentPreview() {
    val isDark = isSystemInDarkTheme()

    NoteThePadTheme(darkTheme = isDark) {
        ColorSelectorBottomSheetContent(
            selectedColor = NoteColors.colors[1].toArgb(),
            selectedBackgroundImage = -1,
            isDarkTheme = isDark,
            onColorClick = {},
            onBackgroundImageClick = {}
        )
    }
}
