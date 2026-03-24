package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews

@Composable
fun AudioRecorderUI(
    isRecording: Boolean,
    onStartRecordingClicked: (Boolean) -> Unit,
    onStopRecordingClicked: (Boolean) -> Unit,
    transcriptionText: String = ""
) {

    var isTranscribeEnabled by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isTranscribeEnabled) {
                    Modifier.fillMaxHeight(0.6f)
                } else {
                    Modifier.wrapContentHeight()
                }
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isTranscribeEnabled) Arrangement.SpaceBetween else Arrangement.Top
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(text = stringResource(R.string.title_audio_recorder),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface)

            Spacer(modifier = Modifier.weight(1f))

            FilledTonalIconButton(
                onClick = { isTranscribeEnabled = !isTranscribeEnabled },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(42.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (isTranscribeEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isTranscribeEnabled)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            ){
                Icon(
                    painter = painterResource(id = R.drawable.speech_to_text_24px),
                    contentDescription = "Transcribe",
                    modifier = Modifier.size(24.dp)
                )
            }
        }


        Spacer(modifier = Modifier.height(24.dp))

        if (isTranscribeEnabled) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Box(
                    modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = transcriptionText.ifEmpty { "Audio to text..." },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (transcriptionText.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Visual indicator (The ECG line we built earlier or a pulse)
//        AudioWaveform(
//            isPlaying = isRecording,
//            modifier = Modifier.fillMaxWidth().height(100.dp)
//        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if(isRecording){
                    onStopRecordingClicked(isTranscribeEnabled)
                } else {
                    onStartRecordingClicked(isTranscribeEnabled)
                }
            } ,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text(if (isRecording) stringResource(R.string.btn_stop_recording) else stringResource(R.string.btn_start_recording))
        }
    }
}


@ThemePreviews
@Composable
fun PreviewAudioRecorderUI(){
    Box(modifier = Modifier.fillMaxSize()) {
        AudioRecorderUI(
            isRecording = true,
            onStartRecordingClicked = {},
            onStopRecordingClicked = {},
            transcriptionText = "This is a test of the Gemini Nano live transcription feature..."
        )
    }
}

@ThemePreviews
@Composable
fun PreviewAudioRecorderUINotRecording(){
    NoteThePadTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AudioRecorderUI(isRecording = false, {}, {})
        }
    }
}