package com.mintanable.notethepad.feature_note.domain.util

import android.net.Uri

enum class AttachmentType(val extension: String) {
    IMAGE("jpg"),
    VIDEO("mp4"),
    AUDIO("mp4")
}

data class Attachment(
    val uri: Uri,
    val duration: Long = 0L
)