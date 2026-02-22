package com.mintanable.notethepad.feature_backup.presentation

data class DriveFileMetadata(
    val id: String,
    val name: String,
    val modifiedTime: Long,
    val size: Long
)