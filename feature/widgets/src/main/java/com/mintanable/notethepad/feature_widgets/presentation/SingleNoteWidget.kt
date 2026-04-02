package com.mintanable.notethepad.feature_widgets.presentation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
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
import androidx.glance.color.ColorProvider
import androidx.glance.layout.ContentScale
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.core.common.NavigationConstants
import com.mintanable.notethepad.core.richtext.serializer.RichTextSerializer
import com.mintanable.notethepad.database.db.entity.DetailedNote
import com.mintanable.notethepad.feature_widgets.R
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

        val note = if (noteId.isNotEmpty()) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context,
                WidgetEntryPoint::class.java
            )
            val noteWithTags = entryPoint.noteRepository().getNoteById(noteId)
            noteWithTags?.let { entryPoint.detailedNoteMapper().toDetailedNote(it.noteEntity, it.tagEntities) }
        } else null

        provideContent {
            val intent = Intent(NavigationConstants.ACTION_OPEN_NOTE).apply {
                component = ComponentName(context.packageName, NavigationConstants.MAIN_ACTIVITY_CLASS)
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
                            id = "",
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

    val noteColorProvider = ColorProvider(
        day = Color(NoteColors.resolveDisplayColor(note.color, false).toArgb()),
        night = Color(NoteColors.resolveDisplayColor(note.color, true).toArgb())
    )

    Box(
        modifier = GlanceModifier.fillMaxSize()
    ){

        if (note.backgroundImage != -1) {
            val backgroundRes = NoteColors.resolveBackgroundImage(note.backgroundImage)

            Image(
                provider = ImageProvider(backgroundRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize()
            )
        } else {
            Spacer(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(
                        imageProvider = ImageProvider(R.drawable.widget_background),
                        colorFilter = ColorFilter.tint(noteColorProvider)
                    )
            )
        }

        LazyColumn(
            modifier = GlanceModifier
                .fillMaxSize()
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
                val spannableContent = remember(note.content) {
                    RichTextSerializer.toSpannable(RichTextSerializer.deserialize(note.content))
                }
                Text(
                    text = spannableContent.toString(),
                    style = NoteListLayoutTextStyles.contentText,
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .maybeClickable(action)
                )
            }
        }
    }
}

private fun GlanceModifier.maybeClickable(action: Action?): GlanceModifier {
    return if (action != null) this.clickable(action) else this
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
