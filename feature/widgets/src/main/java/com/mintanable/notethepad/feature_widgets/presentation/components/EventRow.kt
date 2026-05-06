package com.mintanable.notethepad.feature_widgets.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.compose.ui.graphics.toArgb
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.feature_widgets.presentation.utils.WidgetEvent
import com.mintanable.notethepad.feature_widgets.presentation.utils.NoteListLayoutTextStyles
import com.mintanable.notethepad.feature_widgets.presentation.utils.buildOpenNoteIntent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun EventRow(
    event: WidgetEvent,
    modifier: GlanceModifier = GlanceModifier
) {
    val context = LocalContext.current
    val time = Instant.ofEpochMilli(event.reminderTime)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(timeFormatter)

    val noteColorProvider = ColorProvider(
        day = Color(NoteColors.resolveDisplayColor(event.color, false).toArgb()),
        night = Color(NoteColors.resolveDisplayColor(event.color, true).toArgb())
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable(actionStartActivity(buildOpenNoteIntent(context, noteId = event.noteId))),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = GlanceModifier
                .size(8.dp)
                .cornerRadius(4.dp)
                .background(noteColorProvider)
        )
        Spacer(GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = time,
                style = NoteListLayoutTextStyles.contentText,
                maxLines = 1
            )
            Text(
                text = event.title.ifBlank { "Untitled" },
                style = NoteListLayoutTextStyles.titleText,
                maxLines = 1
            )
        }
    }
}
