package com.mintanable.notethepad.feature_note.presentation.modify.components.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.feature_note.presentation.modify.AddEditNoteEvent
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews

fun LazyListScope.summarySection(
    summary: String,
    onEvent: (AddEditNoteEvent) -> Unit
) {
    item {
        OutlinedCard(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
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
                    .padding(12.dp)
            )
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