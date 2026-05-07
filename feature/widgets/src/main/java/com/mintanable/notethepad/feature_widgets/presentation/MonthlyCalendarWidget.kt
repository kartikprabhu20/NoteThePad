package com.mintanable.notethepad.feature_widgets.presentation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
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
import com.mintanable.notethepad.feature_widgets.presentation.utils.ChangeCalendarMonthAction
import com.mintanable.notethepad.feature_widgets.presentation.utils.LargeWidgetPreview
import com.mintanable.notethepad.feature_widgets.presentation.utils.MonthlyWidgetKeys
import com.mintanable.notethepad.feature_widgets.presentation.utils.NoteListLayoutTextStyles
import com.mintanable.notethepad.feature_widgets.presentation.utils.SelectCalendarDayAction
import com.mintanable.notethepad.feature_widgets.presentation.utils.WidgetEntryPoint
import com.mintanable.notethepad.feature_widgets.presentation.utils.WidgetEvent
import com.mintanable.notethepad.feature_widgets.presentation.utils.changeCalendarMonthActionParams
import com.mintanable.notethepad.feature_widgets.presentation.utils.eventsByDate
import com.mintanable.notethepad.feature_widgets.presentation.utils.eventsForDay
import com.mintanable.notethepad.feature_widgets.presentation.utils.monthGrid
import com.mintanable.notethepad.feature_widgets.presentation.utils.selectCalendarDayActionParams
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

class MonthlyCalendarWidget : GlanceAppWidget() {

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
            val prefs = currentState<Preferences>()
            val storedDate = prefs[MonthlyWidgetKeys.SELECTED_DATE]
            val storedMonth = prefs[MonthlyWidgetKeys.DISPLAYED_MONTH]
            val today = LocalDate.now()
            val selected = parseDate(storedDate) ?: today
            val displayedMonth = parseYearMonth(storedMonth) ?: YearMonth.from(today)

            GlanceTheme {
                MonthlyContent(
                    events = events,
                    today = today,
                    selectedDate = selected,
                    displayedMonth = displayedMonth
                )
            }
        }
    }
}

private fun parseDate(value: String?): LocalDate? =
    value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

private fun parseYearMonth(value: String?): YearMonth? =
    value?.let { runCatching { YearMonth.parse(it) }.getOrNull() }

@Composable
internal fun MonthlyContent(
    events: List<WidgetEvent>,
    today: LocalDate,
    selectedDate: LocalDate,
    displayedMonth: YearMonth
) {
    val context = LocalContext.current
    val grid = monthGrid(displayedMonth)
    val byDate = eventsByDate(events)
    val dayEvents = eventsForDay(events, selectedDate)
    val monthLabel = displayedMonth.format(
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    )

    Scaffold(
        backgroundColor = GlanceTheme.colors.widgetBackground,
        horizontalPadding = 4.dp,
        modifier = GlanceModifier.padding(bottom = 12.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            MonthHeader(monthLabel = monthLabel)
            DayOfWeekRow()
            grid.chunked(7).forEach { weekRow ->
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    weekRow.forEach { date ->
                        DayCell(
                            date = date,
                            isCurrentMonth = YearMonth.from(date) == displayedMonth,
                            isSelected = date == selectedDate,
                            isToday = date == today,
                            hasEvents = byDate.containsKey(date),
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }
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
private fun MonthHeader(monthLabel: String) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = GlanceModifier
                .size(32.dp)
                .cornerRadius(16.dp)
                .clickable(
                    actionRunCallback<ChangeCalendarMonthAction>(
                        parameters = changeCalendarMonthActionParams(-1)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.baseline_chevron_left_24),
                contentDescription = context.getString(R.string.widget_cd_previous_month),
                modifier = GlanceModifier.size(20.dp)
            )
        }
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = monthLabel,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface,
                    textAlign = TextAlign.Center
                )
            )
        }
        Box(
            modifier = GlanceModifier
                .size(32.dp)
                .cornerRadius(16.dp)
                .clickable(
                    actionRunCallback<ChangeCalendarMonthAction>(
                        parameters = changeCalendarMonthActionParams(1)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.baseline_chevron_right_24),
                contentDescription = context.getString(R.string.widget_cd_next_month),
                modifier = GlanceModifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DayOfWeekRow() {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { dow ->
            Text(
                text = dow.getDisplayName(JavaTextStyle.NARROW, Locale.getDefault()),
                modifier = GlanceModifier.defaultWeight().padding(vertical = 2.dp),
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    hasEvents: Boolean,
    modifier: GlanceModifier = GlanceModifier
) {
    val backgroundColor: ColorProvider = when {
        isSelected -> GlanceTheme.colors.primary
        isToday -> GlanceTheme.colors.primaryContainer
        else -> ColorProvider(Color.Transparent)
    }
    val cellBackground = GlanceModifier
        .padding(bottom = 4.dp)
        .padding(horizontal = 6.dp)
        .background(backgroundColor)
        .cornerRadius(10.dp)

    Box(
        modifier = modifier
            .padding(2.dp)
            .clickable(
                actionRunCallback<SelectCalendarDayAction>(
                    parameters = selectCalendarDayActionParams(date.toString())
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier
                .then(cellBackground)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = TextStyle(
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> GlanceTheme.colors.onPrimary
                        !isCurrentMonth -> GlanceTheme.colors.onSurfaceVariant
                        else -> GlanceTheme.colors.onSurface
                    },
                    textAlign = TextAlign.Center
                )
            )
            if (hasEvents) {
                Spacer(
                    modifier = GlanceModifier
                        .padding(top = 1.dp)
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

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 400, heightDp = 600)
@LargeWidgetPreview
@Composable
fun MonthlyContentPreview() {
    val today = LocalDate.now()
    val displayedMonth = YearMonth.from(today)
    val events = listOf(
        WidgetEvent(
            noteId = "1",
            title = "Project Kickoff",
            reminderTime = today.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            color = 0xFF6200EE.toInt()
        ),
        WidgetEvent(
            noteId = "2",
            title = "Doctor Appointment",
            reminderTime = today.plusDays(2).atTime(14, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            color = 0xFF03DAC6.toInt()
        ),
        WidgetEvent(
            noteId = "3",
            title = "Dinner with family",
            reminderTime = today.minusDays(1).atTime(19, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            color = 0xFF018786.toInt()
        )
    )
    GlanceTheme {
        MonthlyContent(
            events = events,
            today = today,
            selectedDate = today,
            displayedMonth = displayedMonth
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview
@Composable
fun DayCellPreview() {
    val today = LocalDate.now()
    GlanceTheme {
        Box(modifier = GlanceModifier.padding(16.dp)) {
            Row {
                DayCell(
                    date = today,
                    isCurrentMonth = true,
                    isSelected = false,
                    isToday = true,
                    hasEvents = true,
                    modifier = GlanceModifier.size(40.dp)
                )
                DayCell(
                    date = today.plusDays(1),
                    isCurrentMonth = true,
                    isSelected = true,
                    isToday = false,
                    hasEvents = false,
                    modifier = GlanceModifier.size(40.dp)
                )
                DayCell(
                    date = today.plusDays(2),
                    isCurrentMonth = true,
                    isSelected = false,
                    isToday = false,
                    hasEvents = true,
                    modifier = GlanceModifier.size(40.dp)
                )
                DayCell(
                    date = today.minusMonths(1).withDayOfMonth(28),
                    isCurrentMonth = false,
                    isSelected = false,
                    isToday = false,
                    hasEvents = false,
                    modifier = GlanceModifier.size(40.dp)
                )
            }
        }
    }
}
