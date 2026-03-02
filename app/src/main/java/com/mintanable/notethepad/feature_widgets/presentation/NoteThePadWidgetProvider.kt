package com.mintanable.notethepad.feature_widgets.presentation


import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class NoteListWidgetProvider : GlanceAppWidgetReceiver(){
    override val glanceAppWidget: GlanceAppWidget = NoteListWidget()
}

class SingleNoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleNoteWidget()
}