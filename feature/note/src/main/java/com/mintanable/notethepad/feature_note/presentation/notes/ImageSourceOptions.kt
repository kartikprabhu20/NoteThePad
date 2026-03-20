package com.mintanable.notethepad.feature_note.presentation.notes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VideoCameraFront
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.mintanable.notethepad.feature_note.R

enum class ImageSourceOptions(
    private val titleRes: Int,
    override val  icon: ImageVector
) : AdditionalOption {
    PHOTO_CAMERA(R.string.option_camera, Icons.Default.PhotoCamera),
    PHOTO_GALLERY(R.string.option_gallery, Icons.Default.Collections);

    override val title: String
        @Composable get() = stringResource(titleRes)
}

enum class VideoSourceOptions(
    private val titleRes: Int,
    override val  icon: ImageVector
) : AdditionalOption {
    VIDEO_CAMERA(R.string.option_video_recorder, Icons.Default.VideoCameraFront),
    VIDEO_GALLERY(R.string.option_gallery, Icons.Default.Collections);

    override val title: String
        @Composable get() = stringResource(titleRes)
}

enum class AudioSourceOptions(
    private val titleRes: Int,
    override val  icon: ImageVector
) : AdditionalOption {
    AUDIO_RECORDER(R.string.option_audio_recorder, Icons.Default.RecordVoiceOver);

    override val title: String
        @Composable get() = stringResource(titleRes)
}