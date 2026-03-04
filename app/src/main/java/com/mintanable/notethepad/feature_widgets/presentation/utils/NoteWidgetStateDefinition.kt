package com.mintanable.notethepad.feature_widgets.presentation.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.glance.state.GlanceStateDefinition
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object NoteWidgetStateDefinition : GlanceStateDefinition<Preferences> {

    val NOTE_ID_KEY = longPreferencesKey("selected_note_id")

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<Preferences> {
        return dataStoreCache.getOrPut(fileKey) {
            PreferenceDataStoreFactory.create {
                context.applicationContext.preferencesDataStoreFile("widget_note_$fileKey")
            }
        }
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return context.applicationContext.preferencesDataStoreFile("widget_note_$fileKey")
    }
}

internal val dataStoreCache = ConcurrentHashMap<String, DataStore<Preferences>>()