package com.mintanable.notethepad.feature_calendar

import java.time.LocalDate

sealed class CalendarEvent {
    data class ChangeViewMode(val mode: CalendarViewMode) : CalendarEvent()
    data object NavigatePrevious : CalendarEvent()
    data object NavigateNext : CalendarEvent()
    data class SelectDay(val date: LocalDate) : CalendarEvent()
    data class SelectTimeSlot(val epochMillis: Long) : CalendarEvent()
    data class UpdateNewNoteTitle(val title: String) : CalendarEvent()
    data object DismissBottomSheet : CalendarEvent()
    data object ConfirmNewNoteSlot : CalendarEvent()
}
