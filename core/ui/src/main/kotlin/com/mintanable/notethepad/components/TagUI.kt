package com.mintanable.notethepad.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.core.model.note.NoteColors
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews

@Composable
fun TagUI(
    imageVector: ImageVector? = null,
    description: String,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    enableDeletion: Boolean = true
){

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        border = BorderStroke(0.5.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            if(imageVector != null) {
                Icon(
                    modifier = Modifier
                        .clickable(onClick = onClick),
                    imageVector = imageVector,
                    contentDescription = "icon",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = description,
                modifier = Modifier
                    .clickable(onClick = onClick),
                color = MaterialTheme.colorScheme.onSurface
            )

            if(enableDeletion) {
                Icon(
                    modifier = Modifier.clickable(
                        onClick = onDelete
                    ),
                    imageVector = Icons.Default.Close,
                    contentDescription = "icon",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@ThemePreviews
@Composable
fun PreviewReminderTag(){
    NoteThePadTheme {
        Box(modifier = Modifier
            .background(NoteColors.colors.get(0))
            .padding(8.dp)
        )
        {
            TagUI(Icons.Default.Notifications,
                "Reminder",
                {}, {})
        }
    }
}