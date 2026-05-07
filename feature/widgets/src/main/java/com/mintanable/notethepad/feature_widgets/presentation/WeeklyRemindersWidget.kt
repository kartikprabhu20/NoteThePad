package com.mintanable.notethepad.feature_widgets.presentation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.datastore.preferences.core.Preferences
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mintanable.notethepad.core.model.note.NoteOrder
import com.mintanable.notethepad.core.model.note.OrderType
import com.mintanable.notethepad.feature_widgets.R
import com.mintanable.notethepad.feature_widgets.presentation.components.EventRow
import com.mintanable.notethepad.feature_widgets.presentation.components.RoundedScrollingLazyColumn
import com.mintanable.notethepad.feature_widgets.presentation.components.WidgetTitleBar
import com.mintanable.notethepad.feature_widgets.presentation.utils.LargeWidgetPreview
import com.mintanable.notethepad.feature_widgets.presentation.utils.NoteListLayoutTextStyles
import com.mintanable.notethepad.feature_widgets.presentation.utils.RefreshNoteWidgetCallback
import com.mintanable.notethepad.feature_widgets.presentation.utils.SelectWeekDayAction
import com.mintanable.notethepad.feature_widgets.presentation.utils.WeeklyWidgetKeys
import com.mintanable.notethepad.feature_widgets.presentation.utils.WidgetEntryPoint
import com.mintanable.notethepad.feature_widgets.presentation.utils.WidgetEvent
import com.mintanable.notethepad.feature_widgets.presentation.utils.eventsByDate
import com.mintanable.notethepad.feature_widgets.presentation.utils.eventsForDay
import com.mintanable.notethepad.feature_widgets.presentation.utils.selectWeekDayActionParams
import com.mintanable.notethepad.feature_widgets.presentation.utils.weekDates
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

class WeeklyRemindersWidget : GlanceAppWidget() {

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
            val storedDate = currentState<Preferences>()[WeeklyWidgetKeys.SELECTED_DATE]
            val today = LocalDate.now()
            val selected = parseLocalDateOrNull(storedDate) ?: today

            GlanceTheme {
                WeeklyContent(
                    events = events,
                    today = today,
                    selectedDate = selected
                )
            }
        }
    }
}

private fun parseLocalDateOrNull(value: String?): LocalDate? =
    value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

@Composable
internal fun WeeklyContent(
    events: List<WidgetEvent>,
    today: LocalDate,
    selectedDate: LocalDate
) {
    val context = LocalContext.current
    val week = weekDates(today)
    val byDate = eventsByDate(events)
    val dayEvents = eventsForDay(events, selectedDate)

    Scaffold(
        titleBar = {
            WidgetTitleBar(
                title = context.getString(R.string.widget_title_weekly_reminders),
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
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        isSelected = date == selectedDate,
                        isToday = date == today,
                        hasEvents = byDate.containsKey(date),
                        modifier = GlanceModifier.defaultWeight()
                    )
                }
            }

            Box(modifier = GlanceModifier.fillMaxSize()) {
                if (dayEvents.isEmpty()) {
                    val weekday = selectedDate.dayOfWeek
                        .getDisplayName(JavaTextStyle.FULL, Locale.getDefault())
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = context.getString(R.string.widget_empty_day, weekday),
                            style = NoteListLayoutTextStyles.contentText
                        )
                    }
                } else {
                    RoundedScrollingLazyColumn(
                        items = dayEvents,
                        itemContentProvider = { event -> EventRow(event) },
                        modifier = GlanceModifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasEvents: Boolean,
    modifier: GlanceModifier = GlanceModifier
) {
    val weekday = date.dayOfWeek
        .getDisplayName(JavaTextStyle.NARROW, Locale.getDefault())
    val dayNum = date.format(DateTimeFormatter.ofPattern("d", Locale.getDefault()))

    val baseModifier = modifier
        .padding(2.dp)
        .clickable(
            actionRunCallback<SelectWeekDayAction>(
                parameters = selectWeekDayActionParams(date.toString())
            )
        )

    val backgroundColor: ColorProvider = when {
        isSelected -> GlanceTheme.colors.primary
        isToday -> GlanceTheme.colors.primaryContainer
        else -> ColorProvider(Color.Transparent)
    }
    val cellBackground = GlanceModifier
        .background(backgroundColor)
        .cornerRadius(12.dp)

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier
                .then(cellBackground)
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = weekday,
                style = TextStyle(
                    fontWeight = FontWeight.Normal,
                    color = if (isSelected) GlanceTheme.colors.onPrimary
                    else GlanceTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = dayNum,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) GlanceTheme.colors.onPrimary
                    else GlanceTheme.colors.onSurface,
                    textAlign = TextAlign.Center
                )
            )
            if (hasEvents) {
                Spacer(
                    modifier = GlanceModifier
                        .padding(top = 2.dp)
                        .size(4.dp)
                        .cornerRadius(2.dp)
                        .background(
                            if (isSelected) GlanceTheme.colors.onPrimary
                            else GlanceTheme.colors.primary
                        )
                )
            }
        }
    }
}

@LargeWidgetPreview
@Composable
fun WeeklyContentPreview() {
    val today = LocalDate.now()
    val events = listOf(
        WidgetEvent(
            noteId = "1",
            title = "Weekly Sync",
            reminderTime = today.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            color = 0xFF6200EE.toInt()
        ),
        WidgetEvent(
            noteId = "2",
            title = "Gym Session",
            reminderTime = today.atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            color = 0xFF03DAC6.toInt()
        ),
        WidgetEvent(
            noteId = "3",
            title = "Grocery Shopping",
            reminderTime = today.plusDays(1).atTime(11, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            color = 0xFF018786.toInt()
        )
    )
    GlanceTheme {
        WeeklyContent(
            events = events,
            today = today,
            selectedDate = today
        )
    }
}
