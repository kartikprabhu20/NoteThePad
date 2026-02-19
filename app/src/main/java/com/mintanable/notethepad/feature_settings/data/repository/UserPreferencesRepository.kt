package com.mintanable.notethepad.feature_settings.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mintanable.notethepad.feature_settings.domain.model.Settings
import com.mintanable.notethepad.feature_settings.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BACKUP_ENABLED = booleanPreferencesKey("backup_enabled")
        val GRID_VIEW_ENABLED =  booleanPreferencesKey("gridview_enabled")
    }

    val settingsFlow: Flow<Settings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val notifications = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
            val theme = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            val backupEnabled = preferences[PreferencesKeys.BACKUP_ENABLED] ?: false
            val isGridViewEnabled = preferences[PreferencesKeys.GRID_VIEW_ENABLED] ?: false

            Settings(
                backupEnabled = backupEnabled,
                notificationsEnabled = notifications,
                themeMode = ThemeMode.valueOf(theme),
                isGridViewSelected = isGridViewEnabled
            )
        }

    suspend fun updateBackup(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKUP_ENABLED] = enabled
        }
    }

    suspend fun updateNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateTheme(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun gridviewEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GRID_VIEW_ENABLED] = enabled
        }
    }
}
