package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_note.presentation.modify.components.audioanimation.AmplitudeBarGraph
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews
import kotlin.random.Random

@Composable
fun SimpleAudioPlayerUI(
    totalDuration: Long,
    columnCount: Int = 2,
) {

    val numberOfBars = if(columnCount==2){ 20 }
    else if(columnCount == 1){40}
    else{8}

    val mockAmplitudes = remember {
        List(numberOfBars) { Random.nextDouble(0.2, 1.0).toFloat() }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
                Icon(
                    modifier = Modifier.padding(4.dp,4.dp,0.dp, 4.dp),
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )

            AmplitudeBarGraph(
                amplitudeLevels = mockAmplitudes,
                progress = 100f,
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                barColor = MaterialTheme.colorScheme.onSurface,
                progressColor = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = formatMillisToTime(totalDuration),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.wrapContentWidth().padding(end = 8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@ThemePreviews
@Composable
fun PreviewSimpleAudioPlayerUi() {
    NoteThePadTheme {
        SimpleAudioPlayerUI(totalDuration = 1234567L,)
    }
}