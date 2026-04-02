package com.mintanable.notethepad.feature_widgets.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle

object NoteListLayoutTextStyles {
    /**
     * Style for the text displayed as title within each item.
     */
    val titleText: TextStyle
        @Composable get() = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp, // M3 Title Medium
            color = ColorProvider(Color.Black, night = Color.White)
        )

    /**
     * Style for the text displayed as supporting text within each item.
     */
    val contentText: TextStyle
        @Composable get() =
            TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp, // M3 Label Medium
                color = ColorProvider(Color.Black, night = Color.White)
            )
}