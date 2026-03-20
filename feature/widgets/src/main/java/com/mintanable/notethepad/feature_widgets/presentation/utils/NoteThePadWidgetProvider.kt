package com.mintanable.notethepad.feature_widgets.presentation.utils


import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import com.mintanable.notethepad.feature_widgets.presentation.NoteListWidget
import com.mintanable.notethepad.feature_widgets.presentation.SingleNoteWidget
import com.mintanable.notethepad.feature_widgets.repository.NoteWidgetPrefs
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class NoteListWidgetProvider : GlanceAppWidgetReceiver(){
    override val glanceAppWidget: GlanceAppWidget = NoteListWidget()
}

class SingleNoteWidgetReceiver : GlanceAppWidgetReceiver() {

    companion object{
        const val PINNING_NOTE_ID = "PINNING_NOTE_ID"
        const val PINNING_ACTION = "WIDGET_PINNED_SUCCESS"
    }

        override val glanceAppWidget: GlanceAppWidget = SingleNoteWidget()

        private val coroutineScope = MainScope()

        override fun onUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            appWidgetIds.forEach { appWidgetId ->
                val noteId = NoteWidgetPrefs.getNoteId(context, appWidgetId)
                if (noteId != -1L) {
                    super.onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
                }
            }
        }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            appWidgetIds?.forEach { appWidgetId ->
                val pendingNoteId = NoteWidgetPrefs.getPendingNoteId(context)
                val existingNoteId = NoteWidgetPrefs.getNoteId(context, appWidgetId)
                if (existingNoteId == -1L && pendingNoteId != -1L) {
                    NoteWidgetPrefs.saveNoteId(context, appWidgetId, pendingNoteId)
                    NoteWidgetPrefs.clearPendingNoteId(context)

                    coroutineScope.launch {
                        SingleNoteWidget().update(context, GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId))
                    }
                } else if (existingNoteId != -1L) {
                    coroutineScope.launch {
                        SingleNoteWidget().update(context, GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId))
                    }
                }
            }
        } else if(intent.action == PINNING_ACTION){
            val noteId = intent.getLongExtra(PINNING_NOTE_ID, -1L)
            if (noteId != -1L){
                NoteWidgetPrefs.savePendingNoteId(context, noteId)
            }

            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (noteId != -1L && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                NoteWidgetPrefs.saveNoteId(context, appWidgetId, noteId)
                NoteWidgetPrefs.clearPendingNoteId(context)
                coroutineScope.launch {
                    SingleNoteWidget().updateAll(context)
                }
            }else {
                //receiver gets onUpdate trigger from os before onReceive from our intent, hence we need to manually trigger update again.
                val updateIntent = Intent(context, SingleNoteWidgetReceiver::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                        ComponentName(
                            context,
                            SingleNoteWidgetReceiver::class.java
                        )
                    )
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(updateIntent)
            }
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