package com.mintanable.notethepad.feature_widgets.presentation

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.Text
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking

class SingleNoteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 1. Get the saved Note ID from this widget's private storage
//        val prefs = currentState<Preferences>()
//        val pinnedId = prefs[intPreferencesKey("pinned_note_id")] ?: -1
//
//        // 2. Fetch the specific note
//        val useCases = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).noteUseCases()
//        val note = if (pinnedId != -1) {
//            // Use runBlocking here as we need the single item snapshot
//            runBlocking { useCases.getNote(pinnedId) }
//        } else null

        provideContent {
            GlanceTheme {
//                if (note != null) {
//                    SingleNoteContent(note)
//                } else {
                    Text("Tap to pin a note")
//                }
            }
        }
    }

}
