package com.mintanable.notethepad.feature_widgets.presentation.utils

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.mintanable.notethepad.feature_widgets.presentation.WeeklyRemindersWidget

object WeeklyWidgetKeys {
    val SELECTED_DATE = stringPreferencesKey("weekly_widget_selected_date")
    val DATE_PARAM = ActionParameters.Key<String>("date")
}

fun selectWeekDayActionParams(date: String): ActionParameters =
    actionParametersOf(WeeklyWidgetKeys.DATE_PARAM to date)

class SelectWeekDayAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val date = parameters[WeeklyWidgetKeys.DATE_PARAM] ?: return
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WeeklyWidgetKeys.SELECTED_DATE] = date
        }
        WeeklyRemindersWidget().updateAll(context)
    }
}
