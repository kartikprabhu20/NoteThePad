package com.mintanable.notethepad.feature_widgets

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import com.mintanable.notethepad.core.common.WidgetRefresher
import com.mintanable.notethepad.feature_widgets.presentation.DailyRemindersWidget
import com.mintanable.notethepad.feature_widgets.presentation.MonthlyCalendarWidget
import com.mintanable.notethepad.feature_widgets.presentation.NoteListWidget
import com.mintanable.notethepad.feature_widgets.presentation.SingleNoteWidget
import com.mintanable.notethepad.feature_widgets.presentation.WeeklyRemindersWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GlanceWidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetRefresher {
    override suspend fun refresh() {
        refreshSafely(NoteListWidget())
        refreshSafely(SingleNoteWidget())
        refreshSafely(DailyRemindersWidget())
        refreshSafely(WeeklyRemindersWidget())
        refreshSafely(MonthlyCalendarWidget())
    }

    private suspend fun refreshSafely(widget: GlanceAppWidget) {
        try {
            widget.updateAll(context)
        } catch (t: Throwable) {
            Log.w("WidgetRefresher", "${widget.javaClass.simpleName} updateAll failed", t)
        }
    }
}
