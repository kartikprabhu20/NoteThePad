package com.mintanable.notethepad.feature_widgets.presentation

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll

class RefreshNoteWidgetCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d("kptest", "RefreshNoteWidgetCallback")
        NoteListWidget().updateAll(context)
    }
}
