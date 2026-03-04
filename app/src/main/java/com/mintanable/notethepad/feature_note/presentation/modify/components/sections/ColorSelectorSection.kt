package com.mintanable.notethepad.feature_note.presentation.modify.components.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_note.domain.model.NoteColors

fun LazyListScope.colorSelectorSection(
    selectedColor: Int,
    onColorClick: (Int) -> Unit
) {
    item(key = "color_selector_section") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NoteColors.colors.forEach { color ->
                val colorInt = color.toArgb()
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .shadow(15.dp, CircleShape)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = 3.dp,
                            color = if (selectedColor == colorInt) {
                                Color.Black
                            } else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable {
                            onColorClick(colorInt)
                        }
                )
            }
        }
    }
}