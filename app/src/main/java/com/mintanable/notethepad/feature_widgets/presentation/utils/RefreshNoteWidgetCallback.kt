package com.mintanable.notethepad.feature_widgets.presentation.utils

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.mintanable.notethepad.feature_widgets.presentation.NoteListWidget

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
