package com.mintanable.notethepad.feature_widgets.presentation.utils

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.mintanable.notethepad.feature_widgets.presentation.MonthlyCalendarWidget

object MonthlyWidgetKeys {
    val SELECTED_DATE = stringPreferencesKey("monthly_widget_selected_date")
    val DISPLAYED_MONTH = stringPreferencesKey("monthly_widget_displayed_month")
    val DATE_PARAM = ActionParameters.Key<String>("date")
    val DELTA_PARAM = ActionParameters.Key<Int>("delta")
}

fun selectCalendarDayActionParams(date: String): ActionParameters =
    actionParametersOf(MonthlyWidgetKeys.DATE_PARAM to date)

class SelectCalendarDayAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val date = parameters[MonthlyWidgetKeys.DATE_PARAM] ?: return
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[MonthlyWidgetKeys.SELECTED_DATE] = date
        }
        MonthlyCalendarWidget().update(context, glanceId)
    }
}
