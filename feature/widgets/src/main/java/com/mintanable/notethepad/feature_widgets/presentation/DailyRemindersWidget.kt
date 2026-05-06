package com.mintanable.notethepad.feature_widgets.presentation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.mintanable.notethepad.core.model.note.NoteOrder
import com.mintanable.notethepad.core.model.note.OrderType
import com.mintanable.notethepad.feature_widgets.R
import com.mintanable.notethepad.feature_widgets.presentation.components.EventRow
import com.mintanable.notethepad.feature_widgets.presentation.components.RoundedScrollingLazyColumn
import com.mintanable.notethepad.feature_widgets.presentation.components.WidgetTitleBar
import com.mintanable.notethepad.feature_widgets.presentation.utils.NoteListLayoutTextStyles
import com.mintanable.notethepad.feature_widgets.presentation.utils.RefreshNoteWidgetCallback
import com.mintanable.notethepad.feature_widgets.presentation.utils.WidgetEntryPoint
import com.mintanable.notethepad.feature_widgets.presentation.utils.WidgetEvent
import com.mintanable.notethepad.feature_widgets.presentation.utils.eventsForDay
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class DailyRemindersWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val repository = entryPoint.noteRepository()

        val eventsFlow = repository.getNotes(NoteOrder.Date(OrderType.Ascending)).map { list ->
            list.mapNotNull { noteWithTags ->
                val note = noteWithTags.noteEntity
                if (note.reminderTime > 0) {
                    WidgetEvent(
                        noteId = note.id,
                        title = note.title,
                        reminderTime = note.reminderTime,
                        color = note.color
                    )
                } else null
            }
        }

        provideContent {
            val events by eventsFlow.collectAsState(initial = emptyList())
            GlanceTheme {
                DailyRemindersContent(events = events)
            }
        }
    }
}

@Composable
private fun DailyRemindersContent(events: List<WidgetEvent>) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val todayLabel = today.format(
        DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
    )
    val todaysEvents = eventsForDay(events, today)

    Scaffold(
        titleBar = {
            WidgetTitleBar(
                title = todayLabel,
                titleIconRes = context.resources.getIdentifier(
                    "notethepad_launcher_round", "mipmap", context.packageName
                ),
                titleBarActionIconRes = R.drawable.baseline_refresh_24,
                titleBarActionIconContentDescription = context.getString(
                    R.string.content_description_refresh_widgets
                )
            ) {
                actionRunCallback<RefreshNoteWidgetCallback>()
            }
        },
        backgroundColor = GlanceTheme.colors.widgetBackground,
        horizontalPadding = 4.dp,
        modifier = GlanceModifier.padding(bottom = 12.dp)
    ) {
        Box(modifier = GlanceModifier.fillMaxSize()) {
            if (todaysEvents.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = context.getString(R.string.widget_empty_today),
                        style = NoteListLayoutTextStyles.contentText
                    )
                }
            } else {
                RoundedScrollingLazyColumn(
                    items = todaysEvents,
                    itemContentProvider = { event -> EventRow(event) },
                    modifier = GlanceModifier.fillMaxSize()
                )
            }
        }
    }
}
