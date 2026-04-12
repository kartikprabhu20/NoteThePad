package com.mintanable.notethepad.core.analytics.internal

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.analyticsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "analytics_prefs",
)

@Singleton
class AnalyticsPreferences(
    private val context: Context,
) {
    private val store: DataStore<Preferences> get() = context.analyticsDataStore

    val analyticsEnabledFlow: Flow<Boolean> = store.data.map { prefs ->
        prefs[KEY_ENABLED] ?: true
    }

    suspend fun getDeviceId(): String {
        val prefs = store.data.first()
        prefs[KEY_DEVICE_ID]?.let { return it }
        val newId = UUID.randomUUID().toString()
        store.edit { it[KEY_DEVICE_ID] = newId }
        return newId
    }

    suspend fun isEnabled(): Boolean = analyticsEnabledFlow.first()

    suspend fun setEnabled(enabled: Boolean) {
        store.edit { it[KEY_ENABLED] = enabled }
    }

    companion object {
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_ENABLED = booleanPreferencesKey("analytics_enabled")
    }
}
