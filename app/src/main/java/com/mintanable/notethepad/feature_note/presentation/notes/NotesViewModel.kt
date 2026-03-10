package com.mintanable.notethepad.feature_note.presentation.notes

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintanable.notethepad.feature_note.domain.model.DetailedNote
import com.mintanable.notethepad.feature_note.domain.model.Tag
import com.mintanable.notethepad.feature_note.domain.use_case.FileIOUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.NoteUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.TagUseCases
import com.mintanable.notethepad.feature_note.domain.util.NoteOrder
import com.mintanable.notethepad.feature_note.domain.util.OrderType
import com.mintanable.notethepad.feature_settings.domain.use_case.GetLayoutSettings
import com.mintanable.notethepad.feature_settings.domain.use_case.ToggleLayoutSettings
import com.mintanable.notethepad.feature_widgets.presentation.NoteListWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteUseCases: NoteUseCases,
    private val tagUseCases: TagUseCases,
    getLayoutSettings: GetLayoutSettings,
    private val toggleLayoutSettings: ToggleLayoutSettings,
    private val fileIOUseCases: FileIOUseCases
) :ViewModel() {

    private var recentlyDeletedNote: DetailedNote? = null

    private val _noteOrder = MutableStateFlow<NoteOrder>(NoteOrder.Date(OrderType.Descending))
    val noteOrder = _noteOrder.asStateFlow()

    private val _showLabelDialog = MutableStateFlow(false)
    val showLabelDialog = _showLabelDialog

    private val _isOrderSectionVisible = MutableStateFlow(false)
    val isOrderSectionVisible = _isOrderSectionVisible.asStateFlow()

    private val _searchInputText: MutableStateFlow<String> =
        MutableStateFlow("")
    val searchInputText = _searchInputText

    private val _triggers = combine(_noteOrder, _searchInputText.debounce(300L).distinctUntilChanged()) { order, query ->
        order to query
    }

    val state: StateFlow<NotesState> = _triggers.flatMapLatest { (order, query) ->
        noteUseCases.getDetailedNotes(order).map { notes ->
            val filtered = if (query.isBlank()) {
                notes
            } else {
                notes.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.content.contains(query, ignoreCase = true)
                }
            }

            NotesState(
                notes = filtered,
            )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotesState()
        )

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

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
                    fileIOUseCases.deleteFiles(event.detailedNote.imageUris.map { it.toString() })
                    noteUseCases.deleteNote(event.detailedNote.toNote())
                    recentlyDeletedNote =  event.detailedNote

                    _eventFlow.emit(
                        UiEvent.ShowSnackbar(
                            message = "Note deleted",
                            actionLabel = "Undo",
                            onAction = { onEvent(NotesEvent.RestoreNote) }
                        )
                    )
                }
            }
            is NotesEvent.RestoreNote ->{
                viewModelScope.launch {
                    val note = recentlyDeletedNote?.toNote()?: return@launch
                    val tags = recentlyDeletedNote?.tags ?: return@launch
                    noteUseCases.saveNoteWithAttachments(note, tags)
                    recentlyDeletedNote = null
                }
            }
            is NotesEvent.PinNote -> {
                viewModelScope.launch {
                    _eventFlow.emit(UiEvent.RequestWidgetPin(event.detailedNote))
                }
            }
            is NotesEvent.ShowLabelDialog -> {
                _showLabelDialog.value = true
            }
            is NotesEvent.DismissLabelDialog -> {
                _showLabelDialog.value = false
            }
            is NotesEvent.EditLabel -> {
                viewModelScope.launch {
                    tagUseCases.saveTag(event.tag)
                }
            }
            is NotesEvent.DeleteLabel -> {
                viewModelScope.launch {
                    tagUseCases.deleteTag(event.tag)
                }
            }
            is NotesEvent.AddLabel -> {
                viewModelScope.launch {
                    tagUseCases.saveTag(Tag(event.tagName))
                    _showLabelDialog.value = false
                }
            }

        }
    }

    fun toggleGridView(enabled: Boolean) {
        viewModelScope.launch { toggleLayoutSettings(enabled) }
    }

    suspend fun updateNoteWidget(context: Context) {
        delay(300)
        NoteListWidget().updateAll(context)
    }

    sealed class UiEvent{
        data class ShowSnackbar(
            val message: String,
            val actionLabel: String? = null,
            val onAction: (() -> Unit)? = null) : UiEvent()
        data class RequestWidgetPin(val note:DetailedNote):UiEvent()
    }
}