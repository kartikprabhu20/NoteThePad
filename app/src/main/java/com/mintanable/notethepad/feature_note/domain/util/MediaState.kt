package com.mintanable.notethepad.feature_note.domain.util

data class MediaState(
    val currentUri: String? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val totalDurationMs: Long = 0L,
    val isBuffering: Boolean = false
)
