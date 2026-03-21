package com.mintanable.notethepad.feature_calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintanable.notethepad.core.model.note.NoteOrder
import com.mintanable.notethepad.core.model.note.OrderType
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.database.helper.DetailedNoteMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val detailedNoteMapper: DetailedNoteMapper
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        observeReminders()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeReminders() {
        viewModelScope.launch {
            noteRepository.getNotes(NoteOrder.Date(OrderType.Descending))
                .mapLatest { notesList ->
                    notesList.map { noteWithTags ->
                        detailedNoteMapper.toDetailedNote(noteWithTags.noteEntity, noteWithTags.tagEntities)
                    }.filter { it.reminderTime > 0 }
                }
                .collect { notes ->
                    _state.update { it.copy(notesWithReminders = notes) }
                }
        }
    }

    fun onEvent(event: CalendarEvent) {
        when (event) {
            is CalendarEvent.ChangeViewMode -> {
                _state.update { it.copy(viewMode = event.mode, selectedDay = null) }
            }
            is CalendarEvent.NavigatePrevious -> {
                _state.update { state ->
                    val newDate = when (state.viewMode) {
                        CalendarViewMode.MONTHLY -> state.currentDate.minusMonths(1)
                        CalendarViewMode.WEEKLY -> state.currentDate.minusWeeks(1)
                        CalendarViewMode.DAILY -> state.currentDate.minusDays(1)
                    }
                    state.copy(currentDate = newDate)
                }
            }
            is CalendarEvent.NavigateNext -> {
                _state.update { state ->
                    val newDate = when (state.viewMode) {
                        CalendarViewMode.MONTHLY -> state.currentDate.plusMonths(1)
                        CalendarViewMode.WEEKLY -> state.currentDate.plusWeeks(1)
                        CalendarViewMode.DAILY -> state.currentDate.plusDays(1)
                    }
                    state.copy(currentDate = newDate)
                }
            }
            is CalendarEvent.SelectDay -> {
                _state.update { it.copy(selectedDay = event.date) }
            }
            is CalendarEvent.SelectTimeSlot -> {
                _state.update { it.copy(showNewNoteSheet = true, selectedSlotTime = event.epochMillis) }
            }
            is CalendarEvent.UpdateNewNoteTitle -> {
                _state.update { it.copy(newNoteTitle = event.title) }
            }
            is CalendarEvent.DismissBottomSheet -> {
                _state.update { it.copy(showNewNoteSheet = false, newNoteTitle = "", selectedSlotTime = -1L) }
            }
            is CalendarEvent.ConfirmNewNoteSlot -> {
                viewModelScope.launch {
                    val time = _state.value.selectedSlotTime
                    val title = _state.value.newNoteTitle
                    _state.update { it.copy(showNewNoteSheet = false, newNoteTitle = "", selectedSlotTime = -1L) }
                    _eventFlow.emit(UiEvent.NavigateToAddNote(reminderTime = time, initialTitle = title))
                }
            }
        }
    }

    sealed class UiEvent {
        data class NavigateToAddNote(val reminderTime: Long, val initialTitle: String) : UiEvent()
    }
}
