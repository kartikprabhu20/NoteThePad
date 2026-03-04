package com.mintanable.notethepad.feature_widgets.repository

import android.content.Context
import androidx.core.content.edit

object NoteWidgetPrefs {

    private const val PREFS_NAME = "note_widget_prefs"
    private const val KEY_PREFIX = "note_id_"

    fun saveNoteId(context: Context, appWidgetId: Int, noteId: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putLong("$KEY_PREFIX$appWidgetId", noteId)
        }
    }

    fun getNoteId(context: Context, appWidgetId: Int): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong("$KEY_PREFIX$appWidgetId", -1L)
    }

    fun clear(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove("$KEY_PREFIX$appWidgetId")
        }
    }
}