package com.mintanable.notethepad.feature_widgets.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

object NotesListDimensions {
    val contentPadding = 12.dp

    val titleTextBreakpoint = 200.dp
    /**
     * Amount of space before the text in each item
     */
    val textStartMargin = 4.dp

    /** Corner radius for image in each item. */
    val imageCornerRadius = 16.dp

    /** Corner radius applied to each item **/
    val itemCornerRadius = 16.dp

    /**
     * Number of columns the grid layout should use to display items in available space.
     */
    val gridCells: Int
        @Composable get() {
            return when (NoteListLayoutSize.fromLocalSize()) {
                NoteListLayoutSize.Medium -> 2
                NoteListLayoutSize.Large -> 3
                else -> 1
            }
        }
}