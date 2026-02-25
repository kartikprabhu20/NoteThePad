package com.mintanable.notethepad.feature_note.domain.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VideoCameraFront
import androidx.compose.ui.graphics.vector.ImageVector

enum class ImageSourceOptions(
    override val title: String,
    override val  icon: ImageVector
) : AdditionalOption {
    PHOTO_CAMERA("Camera", Icons.Default.PhotoCamera),
    PHOTO_GALLERY("Gallery", Icons.Default.Collections),
}

enum class VideoSourceOptions(
    override val title: String,
    override val  icon: ImageVector
) : AdditionalOption {
    VIDEO_CAMERA("Video recorder", Icons.Default.VideoCameraFront),
    VIDEO_GALLERY("Gallery", Icons.Default.Collections)
}

enum class AudioSourceOptions(
    override val title: String,
    override val  icon: ImageVector
) : AdditionalOption {
    AUDIO_RECORDER("Audio recorder", Icons.Default.RecordVoiceOver),
}