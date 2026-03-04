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
import androidx.glance.unit.ColorProvider
import com.mintanable.notethepad.R
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.util.CheckboxConvertors

@Composable
fun IconsRow(note: Note) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        if (note.imageUris.isNotEmpty()) {
            WidgetIcon(R.drawable.baseline_collections_24)
        }
        if (note.audioUris.isNotEmpty()) {
            WidgetIcon(R.drawable.baseline_audiotrack_24)
        }
        if (note.reminderTime > -1) {
            WidgetIcon( if(note.reminderTime> System.currentTimeMillis())
                R.drawable.baseline_notifications_24
            else R.drawable.baseline_notifications_off_24)
        }
        if(CheckboxConvertors.isContentCheckboxList(note.content)){
            WidgetIcon(R.drawable.baseline_check_box_24)
        }
    }
}


@Composable
private fun WidgetIcon(resId: Int) {
    Image(
        provider = ImageProvider(resId),
        contentDescription = null,
        modifier = GlanceModifier.size(24.dp).padding(end = 4.dp),
        colorFilter = ColorFilter.tint(ColorProvider(Color.Black.copy(alpha = 0.5f)))
    )
}