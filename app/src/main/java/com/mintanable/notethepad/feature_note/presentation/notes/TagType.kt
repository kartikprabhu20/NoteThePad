package com.mintanable.notethepad.feature_note.presentation.notes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.vector.ImageVector

enum class TagType(val imageVector: ImageVector) {
    REMINDER_TAG(Icons.Default.Notifications),
    LABEL_TAG(Icons.AutoMirrored.Filled.Label)
}