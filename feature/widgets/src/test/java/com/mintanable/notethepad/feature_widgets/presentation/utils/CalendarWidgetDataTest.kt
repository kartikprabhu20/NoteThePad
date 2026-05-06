package com.mintanable.notethepad.feature_widgets.presentation.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

class CalendarWidgetDataTest {

    private val zone = ZoneId.of("UTC")

    private fun event(id: String, year: Int, month: Int, day: Int, hour: Int = 9): WidgetEvent {
        val millis = ZonedDateTime.of(year, month, day, hour, 0, 0, 0, zone)
            .toInstant().toEpochMilli()
        return WidgetEvent(noteId = id, title = "t-$id", reminderTime = millis, color = 0)
    }

    @Test
    fun `eventsForDay returns only events on that date sorted by time`() {
        val today = LocalDate.of(2026, 5, 6)
        val events = listOf(
            event("a", 2026, 5, 6, hour = 14),
            event("b", 2026, 5, 6, hour = 9),
            event("c", 2026, 5, 7, hour = 9)
        )
        val result = eventsForDay(events, today, zone)

        assertThat(result.map { it.noteId }).containsExactly("b", "a").inOrder()
    }

    @Test
    fun `eventsByDate buckets every event under its local date`() {
        val events = listOf(
            event("a", 2026, 5, 6),
            event("b", 2026, 5, 6),
            event("c", 2026, 5, 7)
        )
        val grouped = eventsByDate(events, zone)

        assertThat(grouped[LocalDate.of(2026, 5, 6)]).hasSize(2)
        assertThat(grouped[LocalDate.of(2026, 5, 7)]).hasSize(1)
    }

    @Test
    fun `weekDates returns Mon to Sun spanning the input date`() {
        val wednesday = LocalDate.of(2026, 5, 6)
        val week = weekDates(wednesday)

        assertThat(week).hasSize(7)
        assertThat(week.first().dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(week.last().dayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
        assertThat(week).contains(wednesday)
    }

    @Test
    fun `weekDates handles a Sunday input as the last day of its week`() {
        val sunday = LocalDate.of(2026, 5, 10)
        val week = weekDates(sunday)

        assertThat(week.last()).isEqualTo(sunday)
        assertThat(week.first()).isEqualTo(LocalDate.of(2026, 5, 4))
    }

    @Test
    fun `monthGrid returns 42 cells starting on a Monday and ending on a Sunday`() {
        val grid = monthGrid(YearMonth.of(2026, 5))

        assertThat(grid).hasSize(42)
        assertThat(grid.first().dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(grid.last().dayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
    }

    @Test
    fun `monthGrid contains every date of the requested month`() {
        val ym = YearMonth.of(2026, 5)
        val grid = monthGrid(ym).toSet()

        for (d in 1..ym.lengthOfMonth()) {
            assertThat(grid).contains(ym.atDay(d))
        }
    }
}
