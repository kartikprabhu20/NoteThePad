package com.mintanable.notethepad.feature_note.presentation.navigationdrawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintanable.notethepad.database.db.entity.DrawerItem
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.use_case.GetNavigationDrawerItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NavigationDrawerViewModel @Inject constructor(
    private val getNavigationDrawerItems: GetNavigationDrawerItems,
    private val noteRepository: NoteRepository
): ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)

    val existingTags = noteRepository.getAllTags()
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val navigationDrawerState: StateFlow<NavigationDrawerState> = _isLoggedIn
        .flatMapLatest { loggedIn ->
            getNavigationDrawerItems(loggedIn)
        }
        .map { items ->
            NavigationDrawerState(items = items)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NavigationDrawerState()
        )

    fun onLoggedIn(isLoggedIn: Boolean) {
        _isLoggedIn.value = isLoggedIn
    }
}

data class NavigationDrawerState(
    val items: List<DrawerItem> = emptyList()
)