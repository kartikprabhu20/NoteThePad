package com.mintanable.notethepad.feature_note.domain.util

data class Attachment(
    val filePath: String,
    val mimeType: String // e.g., "video/mp4" or "image/jpeg"
)

enum class AttachmentType(val extension: String) {
    IMAGE("jpg"),
    VIDEO("mp4"),
    AUDIO("mp4")
}