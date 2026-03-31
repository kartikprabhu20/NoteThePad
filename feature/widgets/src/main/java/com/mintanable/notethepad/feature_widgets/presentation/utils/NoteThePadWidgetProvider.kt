package com.mintanable.notethepad.feature_widgets.presentation.utils


import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.mintanable.notethepad.feature_widgets.presentation.NoteListWidget
import com.mintanable.notethepad.feature_widgets.presentation.SingleNoteWidget
import com.mintanable.notethepad.feature_widgets.repository.NoteWidgetPrefs

class NoteListWidgetProvider : GlanceAppWidgetReceiver(){
    override val glanceAppWidget: GlanceAppWidget = NoteListWidget()
}

class SingleNoteWidgetReceiver : GlanceAppWidgetReceiver() {

    companion object {
        const val PINNING_NOTE_ID = "PINNING_NOTE_ID"
        const val PINNING_ACTION = "WIDGET_PINNED_SUCCESS"
    }

    override val glanceAppWidget: GlanceAppWidget = SingleNoteWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Handle pending note if any
        appWidgetIds.forEach { appWidgetId ->
            val existingNoteId = NoteWidgetPrefs.getNoteId(context, appWidgetId)
            if (existingNoteId.isEmpty()) {
                val pendingNoteId = NoteWidgetPrefs.getPendingNoteId(context)
                if (pendingNoteId.isNotEmpty()) {
                    NoteWidgetPrefs.saveNoteId(context, appWidgetId, pendingNoteId)
                    NoteWidgetPrefs.clearPendingNoteId(context)
                }
            }
        }

        // Only call super.onUpdate once to avoid NPE in Glance's goAsync.
        // BroadcastReceiver.goAsync() can only be called once per broadcast.
        // super.onUpdate triggers updateAll() which handles all instances.
        val idsWithNotes = appWidgetIds.filter {
            NoteWidgetPrefs.getNoteId(context, it).isNotEmpty()
        }

        if (idsWithNotes.isNotEmpty()) {
            super.onUpdate(context, appWidgetManager, idsWithNotes.toIntArray())
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PINNING_ACTION) {
            val noteId = intent.getStringExtra(PINNING_NOTE_ID) ?: ""
            if (noteId.isNotEmpty()) {
                NoteWidgetPrefs.savePendingNoteId(context, noteId)
            }

            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (noteId.isNotEmpty() && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                NoteWidgetPrefs.saveNoteId(context, appWidgetId, noteId)
                NoteWidgetPrefs.clearPendingNoteId(context)
            }

            // Trigger an update manually for this custom action.
            // Using onUpdate ensures we use Glance's internal goAsync safely.
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, javaClass))
            onUpdate(context, manager, ids)
        } else {
            // This will handle ACTION_APPWIDGET_UPDATE and call onUpdate
            super.onReceive(context, intent)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { id ->
            NoteWidgetPrefs.clear(context, id)
        }
        NoteWidgetPrefs.clearPendingNoteId(context)
    }
}
