package com.mintanable.notethepad.feature_navigationdrawer.presentation.navigationdrawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintanable.notethepad.feature_navigationdrawer.domain.model.NavigationDrawerItem
import com.mintanable.notethepad.feature_navigationdrawer.domain.usecase.GetNavigationDrawerItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class NavigationDrawerViewModel @Inject constructor(
    private val getNavigationDrawerItems: GetNavigationDrawerItems
): ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)

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
    val items: List<NavigationDrawerItem> = emptyList()
)