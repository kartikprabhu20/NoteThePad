package com.mintanable.notethepad.feature_widgets.presentation.utils

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.mintanable.notethepad.feature_widgets.presentation.QuickActionWidget

class QuickActionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickActionWidget()
}
