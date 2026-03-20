package com.mintanable.notethepad.feature_note.presentation.modify.components.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_note.presentation.notes.components.TagUI
import com.mintanable.notethepad.feature_note.domain.util.TagType
import com.mintanable.notethepad.feature_note.presentation.notes.util.TimeFormatter

fun LazyListScope.reminderAttachmentSection(
    reminderTime: Long,
    onDelete: () -> Unit,
    onClick: () -> Unit
){
    if(reminderTime != -1L) {
        item {
            Box(
                modifier = Modifier.padding(8.dp)
            ) {
                TagUI(
                    imageVector = TagType.REMINDER_TAG.imageVector,
                    description = TimeFormatter.formatMillis(reminderTime),
                    onDelete = onDelete,
                    onClick = onClick
                )
            }
        }
    }
}