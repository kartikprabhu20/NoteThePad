package com.mintanable.notethepad.feature_note.domain.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomSheetType {
    ATTACH,
    MORE_SETTINGS,
    REMINDER
}

interface AdditionalOption {
    val title: String
    val icon: ImageVector
}

enum class AttachmentOption(
    override val title: String,
    override val icon: ImageVector
) : AdditionalOption {
    Image("Image", Icons.Default.Image),
    Video("Video", Icons.Default.Videocam),
    Audio("Audio", Icons.Default.Mic)
}

enum class ReminderOption(
    override val title: String,
    override val icon: ImageVector
) : AdditionalOption {
    Timer("Timer", Icons.Default.Alarm),
    Calendar("Date", Icons.Default.CalendarToday)
}
