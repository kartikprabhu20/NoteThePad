package com.mintanable.notethepad.feature_widgets.presentation.utils

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.mintanable.notethepad.feature_widgets.presentation.MonthlyCalendarWidget
import java.time.YearMonth

fun changeCalendarMonthActionParams(delta: Int): ActionParameters =
    actionParametersOf(MonthlyWidgetKeys.DELTA_PARAM to delta)

class ChangeCalendarMonthAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val delta = parameters[MonthlyWidgetKeys.DELTA_PARAM] ?: return
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[MonthlyWidgetKeys.DISPLAYED_MONTH]
                ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
                ?: YearMonth.now()
            prefs[MonthlyWidgetKeys.DISPLAYED_MONTH] = current.plusMonths(delta.toLong()).toString()
        }
        MonthlyCalendarWidget().updateAll(context)
    }
}
