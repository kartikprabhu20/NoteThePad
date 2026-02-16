package com.mintanable.notethepad.feature_settings.presentation

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintanable.notethepad.feature_settings.domain.model.Settings
import com.mintanable.notethepad.feature_settings.domain.model.ThemeMode
import com.mintanable.notethepad.feature_settings.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: UserPreferencesRepository
) : ViewModel() {

    val settingsState: StateFlow<Settings> = dataStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Settings()
        )

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch { dataStore.updateNotifications(enabled) }
    }

    fun updateTheme(mode: ThemeMode) {
        viewModelScope.launch { dataStore.updateTheme(mode) }
    }

    fun toggleBackup(enabled: Boolean) {
        viewModelScope.launch { dataStore.updateBackup(enabled) }
    }

}