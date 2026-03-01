package com.mintanable.notethepad.feature_note.presentation.modify.components

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_note.domain.model.NoteColors
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import com.mintanable.notethepad.ui.theme.ThemePreviews

@Composable
fun TagUI(
    imageVector: ImageVector,
    description: String,
    onDelete: () -> Unit,
    onClick: () -> Unit
){

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.5f),
        border = BorderStroke(0.5.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Icon(
                modifier = Modifier.clickable(
                    onClick = onClick
                ),
                imageVector = imageVector,
                contentDescription = "icon"
            )
            Text(modifier = Modifier.clickable(
                onClick = onClick
            ),
                text = description)
            Icon(
                modifier = Modifier.clickable(
                    onClick = onDelete
                ),
                imageVector = Icons.Default.Close,
                contentDescription = "icon"
            )
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