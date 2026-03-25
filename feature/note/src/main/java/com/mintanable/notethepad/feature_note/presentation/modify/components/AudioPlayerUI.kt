package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.database.db.entity.Attachment
import com.mintanable.notethepad.core.model.note.MediaState
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews
import kotlin.random.Random

@Composable
fun AudioPlayerUI(
    attachment: Attachment,
    playbackState: MediaState?,
    onDelete: (String) -> Unit,
    onPlayPause: (String) -> Unit,
    onTranscribe: (String) -> Unit
) {

    val uri = attachment.uri
    val isPlaying = playbackState?.currentUri == uri && playbackState.isPlaying
    val totalDuration = attachment.duration
    val progress = if (playbackState?.currentUri == uri) playbackState.progress else 0f

    AudioPlayer(
        isPlaying = isPlaying,
        progress = progress,
        totalDuration = totalDuration,
        onPlayPause = { onPlayPause(uri) },
        onDelete = { onDelete(uri) },
        onTranscribe = { onTranscribe(uri) }
    )
}

@Composable
fun AudioPlayer(
    isPlaying: Boolean,
    progress: Float,
    totalDuration: Long,
    onPlayPause: () -> Unit,
    onDelete: () -> Unit,
    onTranscribe: () -> Unit
) {

    val mockAmplitudes = remember {
        List(40) { Random.nextDouble(0.2, 1.0).toFloat() }
    }

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
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            AmplitudeBarGraph(
                amplitudeLevels = mockAmplitudes,
                progress = progress,
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .padding(horizontal = 8.dp),
                barColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                progressColor = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = formatMillisToTime(totalDuration),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.wrapContentWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(
                onClick = onTranscribe,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.speech_to_text_24px),
                    contentDescription = stringResource(R.string.content_description_trascribe_audio),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.content_description_remove_audio),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onTranscribe,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.speech_to_text_24px),
                    contentDescription = stringResource(R.string.content_description_trascribe_audio),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@ThemePreviews
@Composable
fun PreviewAudioPlayer(){
    NoteThePadTheme {
        Box(modifier = Modifier.background(NoteColors.colors[2]).padding(8.dp)){
            AudioPlayer(isPlaying = true, progress = 0.5F, totalDuration = 1L, {}, {}, {})

        }
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