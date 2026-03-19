package com.mintanable.notethepad.feature_widgets

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.mintanable.notethepad.core.common.WidgetRefresher
import com.mintanable.notethepad.feature_widgets.presentation.NoteListWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GlanceWidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetRefresher {
    override suspend fun refresh(context: Context) {
        NoteListWidget().updateAll(context)
    }
}
