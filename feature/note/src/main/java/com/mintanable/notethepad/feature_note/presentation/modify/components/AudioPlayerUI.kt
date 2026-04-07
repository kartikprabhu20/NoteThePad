package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import com.mintanable.notethepad.feature_note.presentation.modify.components.audioanimation.AmplitudeBarGraph
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews
import kotlin.random.Random

@Composable
fun AudioPlayerUI(
    attachment: Attachment,
    playbackState: MediaState?,
    onDeleteClicked: (String) -> Unit,
    onPlayPauseClicked: (String) -> Unit,
    onTranscribeClicked: (String) -> Unit,
    isTranscribing: Boolean = false,
    isTranscribeSupported: Boolean = false,
    onAppendToNote: (String) -> Unit = {}
) {

    val uri = attachment.uri
    val isPlaying = playbackState?.currentUri == uri && playbackState.isPlaying
    val totalDuration = attachment.duration
    val progress = if (playbackState?.currentUri == uri) playbackState.progress else 0f
    val transcriptionText = attachment.transcription

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        AudioPlayerPanel(
            isPlaying = isPlaying,
            progress = progress,
            totalDuration = totalDuration,
            onPlayPauseClicked = { onPlayPauseClicked(uri) },
            onDeleteClicked = { onDeleteClicked(uri) },
            onTranscribeClicked = { onTranscribeClicked(uri) },
            isTranscribeSupported = isTranscribeSupported
        )

        if (isTranscribing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if(transcriptionText.isNotEmpty()) {
            Column {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = transcriptionText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (transcriptionText.isEmpty())
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(0.dp,0.dp,16.dp, 16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = { onAppendToNote(transcriptionText) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.btn_add_to_note),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun AudioPlayerPanel(
    isPlaying: Boolean,
    progress: Float,
    totalDuration: Long,
    onPlayPauseClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onTranscribeClicked: () -> Unit,
    isTranscribeSupported: Boolean,
) {

    val mockAmplitudes = remember {
        List(40) { Random.nextDouble(0.2, 1.0).toFloat() }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPlayPauseClicked) {
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
                modifier = Modifier.wrapContentWidth().padding(end = 8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            if(isTranscribeSupported) {
                MagicButton(
                    isVisible = isTranscribeSupported,
                    painter = painterResource(R.drawable.speech_to_text_24px),
                    shape = RoundedCornerShape(4.dp),
                    onButtonClicked = onTranscribeClicked
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onDeleteClicked() }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.content_description_remove_audio),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@ThemePreviews
@Composable
fun PreviewAudioPlayerPanel() {
    NoteThePadTheme {
        Box(modifier = Modifier
            .background(NoteColors.colors[2])
            .padding(8.dp)) {
            AudioPlayerPanel(isPlaying = true, progress = 0.5F, totalDuration = 1L, {}, {}, {}, isTranscribeSupported = true)
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

@ThemePreviews
@Composable
fun PreviewAudioPlayerUI() {
    NoteThePadTheme {
        Box(modifier = Modifier
            .background(NoteColors.colors[1])
            .padding(8.dp)) {
            // Mocking the attachment data
            val mockAttachment = Attachment(
                uri = "content://media/audio/1",
                duration = 125000L, // 02:05
                transcription = "This is a live transcription of the audio note being played back by the Gemini Nano engine..."
            )

            // Mocking a playing state at 40% progress
            val mockPlaybackState = MediaState(
                currentUri = "content://media/audio/1",
                isPlaying = true,
                progress = 0.4f
            )
            AudioPlayerUI(
                attachment = mockAttachment,
                playbackState = mockPlaybackState,
                onDeleteClicked = {},
                onPlayPauseClicked = {},
                onTranscribeClicked = {},
                isTranscribeSupported = true
            )

        }
    }
}

@ThemePreviews
@Composable
fun PreviewAudioPlayerUIEmpty() {
    NoteThePadTheme {
        Box(
            modifier = Modifier
                .background(NoteColors.colors[0])
                .padding(8.dp)
        ) {
            AudioPlayerUI(
                attachment = Attachment(uri = "", duration = 60000L),
                playbackState = null,
                onDeleteClicked = {},
                onPlayPauseClicked = {},
                onTranscribeClicked = {}
            )
        }
    }
}