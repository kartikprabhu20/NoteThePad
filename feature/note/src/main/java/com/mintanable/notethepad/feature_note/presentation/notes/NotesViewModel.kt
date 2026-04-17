package com.mintanable.notethepad.feature_note.presentation.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintanable.notethepad.auth.repository.AuthRepository
import com.mintanable.notethepad.core.common.DispatcherProvider
import com.mintanable.notethepad.core.common.NotesFilterType
import com.mintanable.notethepad.core.common.WidgetRefresher
import com.mintanable.notethepad.core.model.settings.NoteShape
import com.mintanable.notethepad.database.db.entity.DetailedNote
import com.mintanable.notethepad.core.model.note.NoteOrder
import com.mintanable.notethepad.core.model.note.OrderType
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.feature_ai.domain.AiAction
import com.mintanable.notethepad.feature_ai.domain.AiActionBus
import com.mintanable.notethepad.feature_note.domain.use_case.fileio.FileIOUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.GetAiAssistantSettings
import com.mintanable.notethepad.feature_note.domain.use_case.GetNoteShapeSettings
import com.mintanable.notethepad.feature_note.domain.use_case.notes.NoteUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.tags.TagUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.GetSupaSyncSettings
import com.mintanable.notethepad.feature_note.domain.use_case.GetSupaSyncStatus
import com.mintanable.notethepad.feature_note.domain.use_case.RefreshSupaSync
import com.mintanable.notethepad.core.analytics.AnalyticsEvent
import com.mintanable.notethepad.core.analytics.AnalyticsTracker
import com.mintanable.notethepad.feature_note.presentation.notes.NotesViewModel.UiEvent.*
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val noteUseCases: NoteUseCases,
    private val tagUseCases: TagUseCases,
    getNoteShapeSettings: GetNoteShapeSettings,
    getSupaSyncSettings: GetSupaSyncSettings,
    getSupaSyncStatus: GetSupaSyncStatus,
    getAiAssistantSettings: GetAiAssistantSettings,
    private val fileIOUseCases: FileIOUseCases,
    private val dispatchers: DispatcherProvider,
    private val widgetRefresher: WidgetRefresher,
    private val refreshSupaSync: RefreshSupaSync,
    private val authRepository: AuthRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val aiActionBus: AiActionBus,
) : ViewModel() {

    companion object {
        var shouldShowLogoAnimation = true
            private set

        fun markAnimationShown() {
            shouldShowLogoAnimation = false
        }
    }

    val isSupaSyncing: StateFlow<Boolean> = getSupaSyncStatus()
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _filterState = MutableStateFlow(
        DataQuery(
            filter = savedStateHandle.get<String>("filterType") ?: NotesFilterType.ALL.filter,
            tagId = savedStateHandle.get<String>("tagId") ?: "",
            tagName = savedStateHandle.get<String>("tagName") ?: "",
            order = NoteOrder.Date(OrderType.Descending)
        )
    )
    val filterState = _filterState.asStateFlow()

    private val userId = authRepository.getSignedInFirebaseUser()
        .map { it?.uid }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _notesFromDb = combine(_filterState, userId) { query, uId -> query to uId }
        .flatMapLatest { (query, uId) ->
            when (query.filter) {
                NotesFilterType.REMINDERS.filter -> noteUseCases.getNotesWithReminders(query.order)
                NotesFilterType.TAGS.filter -> noteUseCases.getNotesWithTags(query.order, TagEntity(tagId = query.tagId, tagName = query.tagName))
                NotesFilterType.SHARED.filter -> {
                    if (uId != null) noteUseCases.getSharedNotes(uId)
                    else flowOf(emptyList())
                }
                else -> noteUseCases.getDetailedNotes(query.order)
            }
        }.flowOn(dispatchers.io)

    private var recentlyDeletedNote: DetailedNote? = null

    private val _showLabelDialog = MutableStateFlow(false)
    val showLabelDialog = _showLabelDialog.asStateFlow()

    private val _isOrderSectionVisible = MutableStateFlow(false)
    val isOrderSectionVisible = _isOrderSectionVisible.asStateFlow()

    private val _searchInputText = MutableStateFlow("")
    val searchInputText = _searchInputText.asStateFlow()
    private val _debouncedSearchQuery = _searchInputText.debounce(300L).distinctUntilChanged()

    private val _selectedColorCode = MutableStateFlow<Int?>(null)
    val selectedColorCode = _selectedColorCode.asStateFlow()

    init {
        viewModelScope.launch {
            aiActionBus.events.collect { action ->
                handleAiAction(action)
            }
        }
    }

    private fun handleAiAction(action: AiAction) {
        when (action) {
            is AiAction.Search -> {
                _searchInputText.value = action.query
            }
            is AiAction.FilterByColor -> {
                _selectedColorCode.value = action.colorCode
            }
            is AiAction.FilterByTag -> {
                updateFilter(NotesFilterType.TAGS.filter, action.tagId, action.tagName)
            }
            is AiAction.ClearFilters -> {
                _searchInputText.value = ""
                _selectedColorCode.value = null
                updateFilter(NotesFilterType.ALL.filter)
            }
        }
    }

    val state: StateFlow<NotesState> = combine(
        _notesFromDb,
        _debouncedSearchQuery,
        _selectedColorCode
    ) { notes, query, color ->
        var filtered = notes
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true)
            }
        }
        if (color != null) {
            filtered = filtered.filter { it.color == color }
        }

        NotesState(notes = filtered)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotesState()
        )

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _noteToDelete = MutableStateFlow<DetailedNote?>(null)
    val noteToDelete = _noteToDelete.asStateFlow()

    val noteShape: StateFlow<NoteShape> = getNoteShapeSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NoteShape.DEFAULT
        )

    val supaSyncEnabled: StateFlow<Boolean> = getSupaSyncSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val aiAssistantEnabled: StateFlow<Boolean> = getAiAssistantSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun updateFilter(filter: String, tagId: String = "", tagName: String = "") {
        _filterState.value = _filterState.value.copy(
            filter = filter,
            tagId = tagId,
            tagName = tagName
        )
        savedStateHandle["filterType"] = filter
        savedStateHandle["tagId"] = tagId
        savedStateHandle["tagName"] = tagName
    }

    fun onEvent(event: NotesEvent) {
        when (event) {
            is NotesEvent.Order -> {
                _filterState.value = _filterState.value.copy(order = event.noteOrder)
            }

            is NotesEvent.ToggleOrderSection -> {
                _isOrderSectionVisible.value = !_isOrderSectionVisible.value
            }

            is NotesEvent.SearchBarValueChange -> {
                _searchInputText.value = event.searchQuery
            }

            is NotesEvent.DeleteNote -> {
                analyticsTracker.track(AnalyticsEvent.NoteDeleted())
                viewModelScope.launch {
                    _noteToDelete.value = null
                    fileIOUseCases.deleteFiles(event.detailedNote.imageUris.map { it.toString() })
                    noteUseCases.deleteNote(event.detailedNote.toNote())
                    recentlyDeletedNote = event.detailedNote

                    _eventFlow.emit(
                        ShowSnackbar(
                            message = "Note deleted",
                            actionLabel = "Undo",
                            onAction = { onEvent(NotesEvent.RestoreNote) }
                        )
                    )
                }
            }

            is NotesEvent.RequestDeleteNote -> {
                _noteToDelete.value = event.note
            }

            is NotesEvent.RestoreNote -> {
                viewModelScope.launch {
                    val note = recentlyDeletedNote?.toNote() ?: return@launch
                    val tags = recentlyDeletedNote?.tagEntities ?: return@launch
                    noteUseCases.saveNoteWithAttachments(note, tags)
                    recentlyDeletedNote = null
                }
            }

            is NotesEvent.PinNote -> {
                viewModelScope.launch {
                    _eventFlow.emit(RequestWidgetPin(event.detailedNote))
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
                    tagUseCases.saveTag(event.tagEntity)
                }
            }

            is NotesEvent.DeleteLabel -> {
                viewModelScope.launch {
                    tagUseCases.deleteTag(event.tagEntity)
                }
            }

            is NotesEvent.AddLabel -> {
                viewModelScope.launch {
                    tagUseCases.saveTag(TagEntity(tagName = event.tagName))
                    _showLabelDialog.value = false
                }
            }

            NotesEvent.RefreshCloudNotes -> { viewModelScope.launch { refreshSupaSync() } }
        }
    }

    fun refreshWidget() {
        viewModelScope.launch {
            delay(300)
            widgetRefresher.refresh()
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(
            val message: String,
            val actionLabel: String? = null,
            val onAction: (() -> Unit)? = null
        ) : UiEvent()

        data class RequestWidgetPin(val note: DetailedNote) : UiEvent()
    }

    data class DataQuery(val filter: String, val tagId: String, val tagName: String, val order: NoteOrder)
}