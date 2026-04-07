package com.mintanable.notethepad.feature_note.presentation.modify.components.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.feature_note.presentation.modify.AddEditNoteEvent
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews

fun LazyListScope.summarySection(
    summary: String,
    onEvent: (AddEditNoteEvent) -> Unit,
    onDelete: () -> Unit = {}
) {
    item {
        OutlinedCard(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {

            Column {
                Row {
                    Text(
                        stringResource(R.string.summery_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(8.dp, 8.dp, 8.dp, 0.dp).weight(1f)
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(24.dp)
                            .clickable { onDelete() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.content_description_remove),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

                var summaryTFV by remember(summary) { mutableStateOf(TextFieldValue(summary)) }
                BasicTextField(
                    value = summaryTFV,
                    onValueChange = { newValue ->
                        summaryTFV = newValue
                        onEvent(AddEditNoteEvent.EditSummary(newValue.text))
                    },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SummarySectionPreview() {
    val isDark = isSystemInDarkTheme()
    NoteThePadTheme(darkTheme = isDark) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(NoteColors.resolveDisplayColor( NoteColors.colors[1].toArgb(), isDark))
                .padding(16.dp)

        ) {
            summarySection(
                summary = "This is a sample summary for the note. It provides a brief overview of the content.",
                onEvent = {}
            )
        }
    }
}