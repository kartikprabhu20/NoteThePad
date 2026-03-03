package com.mintanable.notethepad.feature_widgets.presentation.utils


import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.mintanable.notethepad.feature_widgets.presentation.NoteListWidget
import com.mintanable.notethepad.feature_widgets.presentation.SingleNoteWidget

class NoteListWidgetProvider : GlanceAppWidgetReceiver(){
    override val glanceAppWidget: GlanceAppWidget = NoteListWidget()
}

class SingleNoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleNoteWidget()
}