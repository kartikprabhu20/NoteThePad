package com.mintanable.notethepad.feature_calendar

import com.mintanable.notethepad.database.db.entity.DetailedNote
import java.time.LocalDate

data class CalendarState(
    val viewMode: CalendarViewMode = CalendarViewMode.MONTHLY,
    val currentDate: LocalDate = LocalDate.now(),
    val selectedDay: LocalDate? = null,
    val notesWithReminders: List<DetailedNote> = emptyList(),
    val showNewNoteSheet: Boolean = false,
    val selectedSlotTime: Long = -1L,
    val newNoteTitle: String = ""
)
