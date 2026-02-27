package com.mintanable.notethepad.feature_note.domain.repository

import android.net.Uri
import com.mintanable.notethepad.feature_note.domain.util.MediaState
import kotlinx.coroutines.flow.Flow

interface MediaPlayer {
    val mediaState: Flow<MediaState>
    fun playPause(uri: Uri)
    fun stop()
    fun release()
}