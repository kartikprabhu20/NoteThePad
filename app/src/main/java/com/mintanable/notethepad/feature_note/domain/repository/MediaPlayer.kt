package com.mintanable.notethepad.feature_note.domain.repository

import android.net.Uri
import com.mintanable.notethepad.feature_note.domain.util.MediaState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MediaPlayer {
    val mediaState: StateFlow<MediaState>
    fun playPause(uri: Uri)
    fun stop()
    fun release()
}