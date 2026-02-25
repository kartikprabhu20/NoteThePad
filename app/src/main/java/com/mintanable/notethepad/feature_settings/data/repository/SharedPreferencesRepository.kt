package com.mintanable.notethepad.feature_settings.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "shared-preference")

class SharedPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val HAS_ASKED_CAMERA = booleanPreferencesKey("has_asked_camera_permission")
    }
    val askedCameraPermission: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.HAS_ASKED_CAMERA] ?: false }

    suspend fun markCameraPermissionRequested() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_ASKED_CAMERA] = true
        }
    }
}