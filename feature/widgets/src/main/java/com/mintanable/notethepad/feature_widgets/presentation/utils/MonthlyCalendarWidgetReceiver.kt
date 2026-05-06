package com.mintanable.notethepad.feature_widgets.presentation.utils

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.mintanable.notethepad.feature_widgets.presentation.MonthlyCalendarWidget

class MonthlyCalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthlyCalendarWidget()
}
