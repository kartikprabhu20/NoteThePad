package com.mintanable.notethepad.feature_widgets.presentation

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import com.mintanable.notethepad.MainActivity
import com.mintanable.notethepad.R
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.model.NoteColors
import com.mintanable.notethepad.feature_note.presentation.notes.util.ReminderReceiver.Companion.TARGET_NOTE_ID
import com.mintanable.notethepad.feature_widgets.presentation.components.IconsRow
import com.mintanable.notethepad.feature_widgets.presentation.utils.GridBreakpointPreviews
import com.mintanable.notethepad.feature_widgets.presentation.utils.LargeWidgetPreview
import com.mintanable.notethepad.feature_widgets.presentation.utils.MediumWidgetPreview
import com.mintanable.notethepad.feature_widgets.presentation.utils.NoteListLayoutTextStyles
import com.mintanable.notethepad.feature_widgets.presentation.utils.SmallWidgetPreview
import com.mintanable.notethepad.feature_widgets.presentation.utils.WidgetEntryPoint
import com.mintanable.notethepad.feature_widgets.repository.NoteWidgetPrefs
import dagger.hilt.android.EntryPointAccessors

class SingleNoteWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val noteId = NoteWidgetPrefs.getNoteId(context, appWidgetId)

        val note = if (noteId != -1L) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context,
                WidgetEntryPoint::class.java
            )
            entryPoint.noteUseCases().getNote(noteId)
        } else null

        provideContent {
            GlanceTheme {
                if (note != null) {
                    NoteItem(note)
                } else {
                    NoteItem(
                        Note(
                            id = null,
                            title = "No note found",
                            content = "Please go to the app and add some notes",
                            timestamp = System.currentTimeMillis(),
                            color = NoteColors.colors[0].toArgb()
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun NoteItem(
    note: Note,
) {
    val context = LocalContext.current
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra(TARGET_NOTE_ID, note.id)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(intent))

    ){
        LazyColumn(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(
                    imageProvider = ImageProvider(R.drawable.widget_background),
                    colorFilter = ColorFilter.tint(ColorProvider(Color(note.color)))
                )
                .padding(8.dp)
        ) {
            item {
                Text(
                    text = note.title,
                    style = NoteListLayoutTextStyles.titleText,
                    maxLines = 2,
                    modifier = GlanceModifier
                        .padding(end = 32.dp)
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
            }

            item{
                IconsRow(note)
                Spacer(modifier = GlanceModifier.height(8.dp))
            }

            item{
                Text(
                    text = note.content,
                    style = NoteListLayoutTextStyles.contentText,
                )
            }

        }
    }
}


@GridBreakpointPreviews
@SmallWidgetPreview
@MediumWidgetPreview
@LargeWidgetPreview
@Composable
private fun NoteListContentPreview() {
    NoteItem(
        Note(
        title = "Remember to call landlord",
        content = "Repairing basin and pipes",
        timestamp = 123,
        color = NoteColors.colors.get(1).toArgb(),
        reminderTime = 1234556
        )
    )
}
