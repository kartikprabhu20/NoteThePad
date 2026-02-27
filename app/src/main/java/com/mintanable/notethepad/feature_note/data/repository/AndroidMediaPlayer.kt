package com.mintanable.notethepad.feature_note.data.repository

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mintanable.notethepad.feature_note.domain.repository.MediaPlayer
import com.mintanable.notethepad.feature_note.domain.util.MediaState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

class AndroidMediaPlayer@Inject constructor(
    @ApplicationContext private val context: Context
) : MediaPlayer {

    val player = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .build()
    private val _mediaState = MutableStateFlow(MediaState())
    override val mediaState = _mediaState.asStateFlow()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _mediaState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(state: Int) {
                _mediaState.update { it.copy(isBuffering = state == Player.STATE_BUFFERING)}

                if (state == Player.STATE_READY) {
                    _mediaState.update { it.copy(totalDurationMs = player.duration.coerceAtLeast(0L)) }
                }

                if (state == Player.STATE_ENDED) {
                    _mediaState.update { it.copy(progress = 0f, isPlaying = false) }
                }
            }
        })
    }

    override fun playPause(uri: Uri) {
        val currentUriString = uri.toString()

        if (_mediaState.value.currentUri == currentUriString) {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
                startProgressUpdate()
            }
            return
        }

        // New file: reset and play
        player.apply {
            stop()
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            play()
        }

        _mediaState.update { it.copy(currentUri = currentUriString) }
        startProgressUpdate()
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(1L)
                    val progress = pos.toFloat() / dur.toFloat()
                    _mediaState.update { it.copy(progress = progress) }
                }
                delay(250)
            }
        }
    }

    override fun stop() {
        progressJob?.cancel()
        player.stop()
        player.clearMediaItems()
        _mediaState.update { MediaState() }
    }

    // ONLY call this if the app is shutting down or you are destroying the Singleton
    override fun release() {
        progressJob?.cancel()
        serviceScope.cancel()
        player.release()
    }
}