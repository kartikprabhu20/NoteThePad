package com.mintanable.notethepad.feature_note.domain.util

import android.net.Uri
import androidx.compose.runtime.Immutable

enum class AttachmentType(val extension: String) {
    IMAGE("jpg"),
    VIDEO("mp4"),
    AUDIO("mp4")
}

@Immutable
data class Attachment(
    val uri: Uri,
    val duration: Long = 0L
)