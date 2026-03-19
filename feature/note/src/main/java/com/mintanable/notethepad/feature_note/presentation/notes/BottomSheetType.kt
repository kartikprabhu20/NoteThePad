package com.mintanable.notethepad.feature_note.presentation.notes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.mintanable.notethepad.feature_note.R

enum class BottomSheetType {
    NONE,
    ATTACH,
    REMINDER,
    MORE_SETTINGS,
    IMAGE_SOURCES,
    VIDEO_SOURCES,
    AUDIO_SOURCES,
    AUDIO_RECORDER,
    CHECKBOX,
    LABEL
}

interface AdditionalOption {
    val title: String
        @Composable get
    val icon: ImageVector
}

enum class AttachmentOptions(
    private val titleRes: Int,
    override val icon: ImageVector
) : AdditionalOption {
    IMAGE(R.string.option_image, Icons.Default.Image),
    VIDEO(R.string.option_video, Icons.Default.Videocam),
    AUDIO(R.string.option_audio, Icons.Default.Mic);

    override val title: String
        @Composable get() = stringResource(titleRes)
}

enum class ReminderOptions(
    private val titleRes: Int,
    override val icon: ImageVector
) : AdditionalOption {
    DATE_AND_TIME(R.string.option_choose_date_time, Icons.Default.AccessTime);

    override val title: String
        @Composable get() = stringResource(titleRes)
}

enum class MoreSettingsOptions(
    private val titleRes: Int,
    override val icon: ImageVector
) : AdditionalOption {
    PIN(R.string.option_pin_widget, Icons.Default.PushPin),
    LABEL(R.string.option_add_label, Icons.AutoMirrored.Filled.Label),
    DELETE(R.string.option_delete, Icons.Default.Delete),
    COPY(R.string.option_make_copy, Icons.Default.ContentCopy);
//    SHARE("Send", Icons.Default.Share)

    override val title: String
        @Composable get() = stringResource(titleRes)
}
