package com.mintanable.notethepad.feature_note.presentation.modify.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import kotlinx.coroutines.delay

@Composable
fun AudioPlayerUI(
    uri: Uri,
    nowPlaying: Boolean,
    onDelete: (Uri) -> Unit,
    onPlayPause: (Uri) -> Unit) {

    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }

    // Update isPlaying state based on player events
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    totalDuration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                currentPosition = exoPlayer.currentPosition
                delay(500)
            }
        }
    }

    LaunchedEffect(nowPlaying) {
        if (nowPlaying) exoPlayer.play() else exoPlayer.pause()
    }


    val progress = if (totalDuration > 0) currentPosition.toFloat() / totalDuration.toFloat() else 0f
    AudioPlayer(
        isPlaying = isPlaying,
        progress = progress,
        totalDuration = totalDuration,
        onPlayPause = {
            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            onPlayPause(uri)
        },
        onDelete = {
            onDelete(uri)
        },
    )
}

@Composable
fun AudioPlayer(
    isPlaying: Boolean,
    progress: Float,
    totalDuration: Long,
    onPlayPause: () -> Unit,
    onDelete: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ){
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
            }

            LinearProgressIndicator( progress = { progress},
                modifier = Modifier.padding(end = 4.dp))

            Text(formatMillisToTime(totalDuration))

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null
                )
            }
        }
    }

}

@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewAudioPlayer(){
    NoteThePadTheme {
        AudioPlayer(isPlaying = true, progress = 0.5F, totalDuration = 1L, {}, {})
    }
}

fun formatMillisToTime(ms: Long): String {
    if (ms <= 0L) return "00:00"

    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    return if (hours > 0) {
        // Format: 01:30:15
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        // Format: 05:45
        "%02d:%02d".format(minutes, seconds)
    }
}