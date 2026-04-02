package com.mintanable.notethepad.feature_widgets.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.color.ColorProvider
import com.mintanable.notethepad.core.common.CheckboxConvertors
import com.mintanable.notethepad.database.db.entity.DetailedNote
import com.mintanable.notethepad.feature_widgets.R

@Composable
fun IconsRow(
    note: DetailedNote,
    modifier: GlanceModifier = GlanceModifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        if (note.imageUris.isNotEmpty()) {
            WidgetIcon(R.drawable.baseline_collections_24)
        }
        if (note.audioAttachments.isNotEmpty()) {
            WidgetIcon(R.drawable.baseline_mic_24)
        }
        if (note.reminderTime > -1) {
            WidgetIcon( if(note.reminderTime> System.currentTimeMillis())
                R.drawable.baseline_notifications_24
            else R.drawable.baseline_notifications_off_24)
        }
        if(CheckboxConvertors.isContentCheckboxList(note.content)){
            WidgetIcon(R.drawable.baseline_checklist_24)
        }
    }
}


@Composable
private fun WidgetIcon(resId: Int) {
    val provider = ColorProvider(
        day = Color.Black,
        night = Color.White
    )
    Image(
        provider = ImageProvider(resId),
        contentDescription = null,
        modifier = GlanceModifier.size(24.dp).padding(end = 4.dp),
        colorFilter = ColorFilter.tint(provider)    )
}