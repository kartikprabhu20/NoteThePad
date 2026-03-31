package com.mintanable.notethepad.feature_widgets.repository

import android.content.Context
import androidx.core.content.edit

object NoteWidgetPrefs {

    private const val PREFS_NAME = "note_widget_prefs"
    private const val KEY_PREFIX = "note_id_"
    private const val PENDING_NOTE_ID = "pending_note_id"

    fun saveNoteId(context: Context, appWidgetId: Int, noteId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString("$KEY_PREFIX$appWidgetId", noteId)
        }
    }

    fun getNoteId(context: Context, appWidgetId: Int): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("$KEY_PREFIX$appWidgetId", "") ?: ""
    }

    fun savePendingNoteId(context: Context, noteId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit(commit = true) {
                putString(PENDING_NOTE_ID, noteId)
            }
    }

    fun getPendingNoteId(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PENDING_NOTE_ID, "") ?: ""
    }

    fun clearPendingNoteId(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove(PENDING_NOTE_ID)
        }
    }

    fun clear(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove("$KEY_PREFIX$appWidgetId")
        }
    }
}