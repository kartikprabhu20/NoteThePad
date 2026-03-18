package com.mintanable.notethepad.feature_widgets.presentation

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.Action
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
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.core.common.NavigationConstants
import com.mintanable.notethepad.core.model.DetailedNote
import com.mintanable.notethepad.core.model.NoteColors
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
            entryPoint.noteUseCases().getDetailedNote(noteId)
        } else null


        provideContent {
            val intent = Intent(NavigationConstants.ACTION_OPEN_NOTE).apply {
                setPackage(context.packageName)
                if (note != null) {
                    putExtra(NavigationConstants.EXTRA_NOTE_ID, note.id)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            GlanceTheme {
                Box(modifier = GlanceModifier.fillMaxSize()) {
                if (note != null) {
                    NoteItem(note, actionStartActivity(intent))
                } else {
                    NoteItem(
                        DetailedNote(
                            id = 0L,
                            title = context.getString(R.string.msg_no_note_found),
                            content = context.getString(R.string.msg_add_notes_prompt),
                            timestamp = System.currentTimeMillis(),
                            color = NoteColors.colors[0].toArgb()
                        ),
                        null
                    )
                }
                }
            }
        }
    }
}

@Composable
fun NoteItem(
    note: DetailedNote,
    action: Action?
) {

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
    ){
        LazyColumn(
            modifier = GlanceModifier
                .fillMaxSize()
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
                        .maybeClickable(action)
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
            }

            item{
                IconsRow(
                    note,
                    modifier = GlanceModifier.maybeClickable(action)
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
            }

            item{
                Text(
                    text = note.content,
                    style = NoteListLayoutTextStyles.contentText,
                    modifier = GlanceModifier
                        .maybeClickable(action)
                )
            }
        }
    }
}

private fun GlanceModifier.maybeClickable(action: Action?): GlanceModifier {
    return if (action != null) {
        Log.d("kptest", "maybeClickable")
        this.clickable(action)
    } else {
        this
    }
}

@GridBreakpointPreviews
@SmallWidgetPreview
@MediumWidgetPreview
@LargeWidgetPreview
@Composable
private fun NoteListContentPreview() {
    NoteItem(
        DetailedNote(
        title = "Remember to call landlord",
        content = "Repairing basin and pipes",
        timestamp = 123,
        color = NoteColors.colors.get(1).toArgb(),
        reminderTime = 1234556
        ),
        null
    )
}
