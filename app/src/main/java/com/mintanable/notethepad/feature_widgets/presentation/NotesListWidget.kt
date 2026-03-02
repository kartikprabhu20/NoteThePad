package com.mintanable.notethepad.feature_widgets.presentation

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.*
import com.mintanable.notethepad.MainActivity
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.util.NoteOrder
import com.mintanable.notethepad.feature_note.domain.util.OrderType
import com.mintanable.notethepad.feature_note.presentation.notes.util.ReminderReceiver.Companion.TARGET_NOTE_ID
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import androidx.glance.text.Text
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.mintanable.notethepad.R
import com.mintanable.notethepad.feature_note.domain.util.CheckboxConvertors

class NoteListWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        val noteUseCases = EntryPointAccessors.fromApplication(
            context, WidgetEntryPoint::class.java
        ).noteUseCases()

        provideContent {

            val notes by noteUseCases.getTopNotes(10)
                .collectAsState(initial = emptyList())

            GlanceTheme {
                NoteListContent(notes)
            }
        }
    }

}

@Composable
fun NoteListContent(notes: List<Note>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(8.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Notes",
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GlanceTheme.colors.onBackground
                )
            )

            Image(
                provider = ImageProvider(R.drawable.baseline_refresh_24),
                contentDescription = "Refresh",
                modifier = GlanceModifier
                    .size(24.dp)
                    .clickable(actionRunCallback<RefreshNoteWidgetCallback>()),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground)
            )
        }

        Spacer(GlanceModifier.height(8.dp))

        LazyColumn {
            items(notes) { note ->
                NoteItemRow(note)
            }
        }
    }
}

@Composable
fun NoteItemRow(note: Note) {
    val context = LocalContext.current
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra(TARGET_NOTE_ID, note.id)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }


    val estimatedHeight = rememberNoteHeight(note)

    val backgroundColor = try {
        Color(note.color)
    } catch (e: Exception) {
        Color.Gray
    }

    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(estimatedHeight)
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(
                    imageProvider = ImageProvider(R.drawable.widget_background),
                    colorFilter = ColorFilter.tint(ColorProvider(backgroundColor))
                )
                .clickable(actionStartActivity(intent))
        ) {

            Column(modifier = GlanceModifier.fillMaxWidth().padding(8.dp)) {
                Text(
                    text = note.title,
                    style = TextStyle(
                        color = ColorProvider(Color.Black),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = note.content,
                    style = TextStyle(
                        color = ColorProvider(Color.Black),
                        fontSize = 12.sp
                    ),
                    maxLines = 5
                )
                Spacer(GlanceModifier.height(4.dp))

                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    if (note.imageUris.isNotEmpty()) {
                        WidgetIcon(R.drawable.baseline_collections_24)
                    }
                    if (note.audioUris.isNotEmpty()) {
                        WidgetIcon(R.drawable.baseline_audiotrack_24)
                    }
                    if (note.reminderTime > -1) {
                        WidgetIcon( if(note.reminderTime> System.currentTimeMillis())
                            R.drawable.baseline_notifications_24
                        else R.drawable.baseline_notifications_off_24)
                    }
                    if(CheckboxConvertors.isContentCheckboxList(note.content)){
                        WidgetIcon(R.drawable.baseline_check_box_24)
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetIcon(resId: Int) {
    Image(
        provider = ImageProvider(resId),
        contentDescription = null,
        modifier = GlanceModifier.size(24.dp).padding(end = 4.dp),
        colorFilter = ColorFilter.tint(ColorProvider(Color.Black.copy(alpha = 0.5f)))
    )
}

@Composable
fun rememberNoteHeight(note: Note): Dp {
    val baseHeight = 60.dp  // title + padding + icons row
    val contentLines = note.content.length / 30  // rough chars-per-line estimate
    val clampedLines = contentLines.coerceIn(1, 5)
    val contentHeight = (clampedLines * 18).dp  // 18.dp per line at 12sp
    val hasIcons = note.imageUris.isNotEmpty() ||
            note.audioUris.isNotEmpty() ||
            note.reminderTime > -1
    val iconsHeight = if (hasIcons) 28.dp else 0.dp

    return baseHeight + contentHeight + iconsHeight
}