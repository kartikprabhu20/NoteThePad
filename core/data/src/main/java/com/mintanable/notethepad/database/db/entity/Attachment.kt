package com.mintanable.notethepad.database.db.entity

import androidx.compose.runtime.Immutable

enum class AttachmentType(val extension: String) {
    IMAGE("jpg"),
    VIDEO("mp4"),
    AUDIO("wav")
}

@Immutable
data class Attachment(
    val uri: String,
    val duration: Long = 0L,
    val transcription: String = ""
)