package com.mintanable.notethepad.feature_widgets.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.LocalSize

enum class NoteListLayoutSize(val maxWidth: Dp) {
    Small(maxWidth = 320.dp),
    Medium(maxWidth = 519.dp),
    Large(maxWidth = Dp.Infinity);

    companion object {
        @Composable
        fun fromLocalSize(): NoteListLayoutSize {
            val size = LocalSize.current

            entries.forEach {
                if (size.width < it.maxWidth) {
                    return it
                }
            }
            throw IllegalStateException("No mapped size ")
        }
    }
}