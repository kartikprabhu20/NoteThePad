package com.mintanable.notethepad.feature_navigationdrawer.presentation.navigationdrawer

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintanable.notethepad.feature_navigationdrawer.domain.model.NavigationDrawerItem
import com.mintanable.notethepad.feature_navigationdrawer.domain.usecase.GetNavigationDrawerItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class NavigationDrawerViewModel @Inject constructor(
    private val getNavigationDrawerItems: GetNavigationDrawerItems
): ViewModel() {

    private val _navigationDrawerState = mutableStateOf(NavigationDrawerState())
    val navigationDrawerState : State<NavigationDrawerState> = _navigationDrawerState

    private var getNavigationDrawerItemsJob: Job? = null


    init {
        collectNavigationDrawerItemList()
    }

    private fun collectNavigationDrawerItemList() {
        getNavigationDrawerItemsJob?.cancel()
        getNavigationDrawerItemsJob = getNavigationDrawerItems(isLoggedIn = false)
            .onEach { items ->

                _navigationDrawerState.value = navigationDrawerState.value.copy(
                    items = items,
                )
            }.launchIn(viewModelScope)
    }
}

data class NavigationDrawerState(
    val items: List<NavigationDrawerItem> = emptyList()
)