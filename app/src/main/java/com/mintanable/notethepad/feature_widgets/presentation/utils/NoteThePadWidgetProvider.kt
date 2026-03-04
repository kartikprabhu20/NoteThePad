package com.mintanable.notethepad.feature_widgets.presentation.utils


import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.mintanable.notethepad.feature_widgets.presentation.NoteListWidget
import com.mintanable.notethepad.feature_widgets.presentation.SingleNoteWidget
import com.mintanable.notethepad.feature_widgets.repository.NoteWidgetPrefs
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class NoteListWidgetProvider : GlanceAppWidgetReceiver(){
    override val glanceAppWidget: GlanceAppWidget = NoteListWidget()
}

class SingleNoteWidgetReceiver : GlanceAppWidgetReceiver() {
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
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, -1)
            if (appWidgetId != -1) {
                coroutineScope.launch {
                    val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                    SingleNoteWidget().update(context, glanceId)
                }
            }
        }
    }

        override fun onDeleted(context: Context, appWidgetIds: IntArray) {
            super.onDeleted(context, appWidgetIds)
            appWidgetIds.forEach { id ->
                NoteWidgetPrefs.clear(context, id)
            }
        }
    }