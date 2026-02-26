package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import com.mintanable.notethepad.ui.theme.ThemePreviews

@Composable
fun AudioRecorderUI(
    isRecording: Boolean,
    onStartRecordingClicked: () -> Unit,
    onStopRecordingClicked: () -> Unit,
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Audio Recorder",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface)

        Spacer(modifier = Modifier.height(24.dp))

        // Visual indicator (The ECG line we built earlier or a pulse)
//        AudioWaveform(
//            isPlaying = isRecording,
//            modifier = Modifier.fillMaxWidth().height(100.dp)
//        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if(isRecording){
                    onStopRecordingClicked()
                } else {
                    onStartRecordingClicked()
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
            Text(if (isRecording) "Stop Recording" else "Start Recording")
        }
    }
}


@ThemePreviews
@Composable
fun PreviewAudioRecorderUI(){
    NoteThePadTheme {
        AudioRecorderUI(isRecording = true, {}, {})
    }
}

@ThemePreviews
@Composable
fun PreviewAudioRecorderUINotRecording(){
    NoteThePadTheme {
        AudioRecorderUI(isRecording = false, {}, {})
    }
}