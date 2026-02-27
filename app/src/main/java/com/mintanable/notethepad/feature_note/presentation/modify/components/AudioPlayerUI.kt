package com.mintanable.notethepad.feature_note.presentation.modify.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_note.domain.util.Attachment
import com.mintanable.notethepad.feature_note.domain.util.MediaState
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import com.mintanable.notethepad.ui.theme.ThemePreviews

@Composable
fun AudioPlayerUI(
    attachment: Attachment,
    playbackState: MediaState?,
    onDelete: (Uri) -> Unit,
    onPlayPause: (Uri) -> Unit) {

    val uri = attachment.uri
    val isPlaying = playbackState?.currentUri == uri.toString() && playbackState.isPlaying
    val totalDuration = attachment.duration
    val progress = if (playbackState?.currentUri == uri.toString()) playbackState.progress else 0f

    AudioPlayer(
        isPlaying = isPlaying,
        progress = progress,
        totalDuration = totalDuration,
        onPlayPause = {
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

            LinearProgressIndicator(
                progress = { progress},
                modifier = Modifier
                    .padding(end = 12.dp)
                    .weight(1f),
                strokeCap = StrokeCap.Round
            )

            Text(
                text = formatMillisToTime(totalDuration),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.widthIn(min = 45.dp)
            )


            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove audio"
                )
            }
        }
    }

}

@ThemePreviews
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