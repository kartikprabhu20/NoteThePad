package com.mintanable.notethepad.features.presentation.notes

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintanable.notethepad.features.domain.model.Note
import com.mintanable.notethepad.features.domain.use_case.NoteUseCases
import com.mintanable.notethepad.features.domain.util.NoteOrder
import com.mintanable.notethepad.features.domain.util.OrderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteUseCases: NoteUseCases
) :ViewModel() {

    private val _state = mutableStateOf(NotesState())
    val state : State<NotesState> = _state
    private var recentlyDeletedNote: Note? = null
    private var getNotesJob : Job? = null

    private val _searchInputText: MutableStateFlow<String> =
        MutableStateFlow("")
    val searchInputText: StateFlow<String> = _searchInputText

    init {
        getNotes(NoteOrder.Date(OrderType.Descending))
    }

    fun onEvent(event:NotesEvent){
        when(event){
            is NotesEvent.Order ->{
                if(state.value.noteOrder::class == event.noteOrder::class &&
                        state.value.noteOrder.orderType == event.noteOrder.orderType){
                    return
                }
                getNotes(event.noteOrder, searchInputText.value)
            }
            is NotesEvent.DeleteNote ->{
                viewModelScope.launch {
                    noteUseCases.deleteNote(event.note)
                    recentlyDeletedNote =  event.note
                }
            }
            is NotesEvent.RestoreNote ->{
                viewModelScope.launch {
                    noteUseCases.addNote(recentlyDeletedNote?: return@launch)
                    recentlyDeletedNote = null
                }
            }
            is NotesEvent.ToggleOrderSection ->{
                _state.value = state.value.copy(
                    isOrderSectionVisible = !state.value.isOrderSectionVisible
                )
            }
            is NotesEvent.SearchBarValueChange -> {
                _searchInputText.value = event.searchQuery
                getNotes(state.value.noteOrder, event.searchQuery)
            }

            else -> {}
        }
    }

    private fun getNotes(noteOrder: NoteOrder, searchQuery: String = "") {
        getNotesJob?.cancel()
        getNotesJob = noteUseCases.getNotes(noteOrder)
            .onEach { notes ->
                val filteredNotes = if (searchQuery.isBlank()) {
                    notes
                } else {
                    notes.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                                it.content.contains(searchQuery, ignoreCase = true)
                    }
                }
                _state.value = state.value.copy(
                    notes = filteredNotes,
                    noteOrder = noteOrder
                )
        }.launchIn(viewModelScope)
    }

}

