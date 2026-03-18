package com.mintanable.notethepad.core.model

data class DriveFileMetadata(
    val id: String,
    val name: String,
    val modifiedTime: Long,
    val size: Long
)