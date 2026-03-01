package com.mintanable.notethepad.feature_note.presentation.notes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomSheetType {
    NONE,
    ATTACH,
    REMINDER,
    MORE_SETTINGS,
    IMAGE_SOURCES,
    VIDEO_SOURCES,
    AUDIO_SOURCES,
    AUDIO_RECORDER,
    CHECKBOX
}

interface AdditionalOption {
    val title: String
    val icon: ImageVector
}

enum class AttachmentOptions(
    override val title: String,
    override val icon: ImageVector
) : AdditionalOption {
    IMAGE("Image", Icons.Default.Image),
    VIDEO("Video", Icons.Default.Videocam),
    AUDIO("Audio", Icons.Default.Mic)
}

enum class ReminderOptions(
    override val title: String,
    override val icon: ImageVector
) : AdditionalOption {
    DATE_AND_TIME("Choose a data & time", Icons.Default.AccessTime),
}

enum class MoreSettingsOptions(
    override val title: String,
    override val icon: ImageVector
) : AdditionalOption {
    DELETE("Delete", Icons.Default.Delete),
    COPY("Make a copy", Icons.Default.ContentCopy),
    SHARE("Send", Icons.Default.Share),
//    LABEL("Label", Icons.AutoMirrored.Filled.Label)
}
