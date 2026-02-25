package com.mintanable.notethepad.feature_note.presentation.notes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintanable.notethepad.core.file.FileManager
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.use_case.NoteUseCases
import com.mintanable.notethepad.feature_note.domain.util.NoteOrder
import com.mintanable.notethepad.feature_note.domain.util.OrderType
import com.mintanable.notethepad.feature_settings.domain.use_case.GetLayoutSettings
import com.mintanable.notethepad.feature_settings.domain.use_case.ToggleLayoutSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteUseCases: NoteUseCases,
    getLayoutSettings: GetLayoutSettings,
    private val toggleLayoutSettings: ToggleLayoutSettings,
    private val fileManager: FileManager
) :ViewModel() {

    private var recentlyDeletedNote: Note? = null

    private val _noteOrder = MutableStateFlow<NoteOrder>(NoteOrder.Date(OrderType.Descending))
    val noteOrder = _noteOrder.asStateFlow()

    private val _isOrderSectionVisible = MutableStateFlow(false)
    val isOrderSectionVisible = _isOrderSectionVisible.asStateFlow()

    private val _searchInputText: MutableStateFlow<String> =
        MutableStateFlow("")
    val searchInputText = _searchInputText

    val state: StateFlow<NotesState> = combine(
        _noteOrder,
        _searchInputText.debounce(300L).distinctUntilChanged(),
        _noteOrder.flatMapLatest { order ->
            noteUseCases.getNotes(order)
        }
    ) { order, query, notes ->
        Log.d("kptest", "order: $order, query: $query")
        val filtered =
            if (query.isBlank()) {
                notes
            }
            else {
                notes.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.content.contains(query, ignoreCase = true)
                }
            }

        NotesState(
            notes = filtered
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotesState()
        )

    val isGridViewEnabled: StateFlow<Boolean> = getLayoutSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun onEvent(event:NotesEvent){
        when(event){
            is NotesEvent.Order ->{ _noteOrder.value = event.noteOrder }
            is NotesEvent.ToggleOrderSection ->{ _isOrderSectionVisible.value = !_isOrderSectionVisible.value }
            is NotesEvent.SearchBarValueChange -> { _searchInputText.value = event.searchQuery }
            is NotesEvent.DeleteNote ->{
                viewModelScope.launch {
                    fileManager.deleteFilesFromPaths(event.note.imageUris)
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
        }
    }

    fun toggleGridView(enabled: Boolean) {
        viewModelScope.launch { toggleLayoutSettings(enabled) }
    }
}