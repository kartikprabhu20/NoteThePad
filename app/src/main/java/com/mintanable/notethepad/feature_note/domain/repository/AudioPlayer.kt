package com.mintanable.notethepad.feature_note.domain.repository

import android.net.Uri
import com.mintanable.notethepad.feature_note.domain.util.AudioState
import kotlinx.coroutines.flow.Flow

interface AudioPlayer {
    val audioState: Flow<AudioState>
    fun playPause(uri: Uri)
    fun stop()
    fun release()
}