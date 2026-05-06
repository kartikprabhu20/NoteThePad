package com.mintanable.notethepad.feature_widgets.presentation.utils

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class WidgetEvent(
    val noteId: String,
    val title: String,
    val reminderTime: Long,
    val color: Int
)

fun toLocalDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()

fun eventsForDay(
    events: List<WidgetEvent>,
    date: LocalDate,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<WidgetEvent> =
    events.filter { toLocalDate(it.reminderTime, zoneId) == date }
        .sortedBy { it.reminderTime }

fun eventsByDate(
    events: List<WidgetEvent>,
    zoneId: ZoneId = ZoneId.systemDefault()
): Map<LocalDate, List<WidgetEvent>> =
    events.groupBy { toLocalDate(it.reminderTime, zoneId) }

fun weekDates(today: LocalDate): List<LocalDate> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0..6).map { monday.plusDays(it.toLong()) }
}

fun monthGrid(yearMonth: YearMonth): List<LocalDate> {
    val firstOfMonth = yearMonth.atDay(1)
    val gridStart = firstOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0 until 42).map { gridStart.plusDays(it.toLong()) }
}
