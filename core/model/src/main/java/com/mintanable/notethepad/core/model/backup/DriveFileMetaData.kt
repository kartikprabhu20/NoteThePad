package com.mintanable.notethepad.core.model.backup

data class DriveFileMetadata(
    val id: String,
    val name: String,
    val modifiedTime: Long,
    val size: Long
)