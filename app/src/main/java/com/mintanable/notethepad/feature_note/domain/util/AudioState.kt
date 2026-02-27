package com.mintanable.notethepad.feature_note.domain.util

data class AudioState(
    val currentUri: String? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val totalDurationMs: Long = 0L,
    val isBuffering: Boolean = false
)
