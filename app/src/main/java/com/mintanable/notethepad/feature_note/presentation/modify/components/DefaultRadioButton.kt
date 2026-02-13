package com.mintanable.notethepad.features.presentation.modify.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.ui.theme.NoteThePadTheme

@Composable
fun DefaultRadioButton(
    text: String,
    selected:Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
){
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ){
        RadioButton(selected = selected, 
            onClick = onSelect, 
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor =  MaterialTheme.colorScheme.onSurfaceVariant

            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DefaultRadioButtonPreview() {
    // We use a wrapper to show both states at once
    NoteThePadTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            // Selected state
            DefaultRadioButton(
                text = "Selected Option",
                selected = true,
                onSelect = { }
            )

            // Unselected state
            DefaultRadioButton(
                text = "Unselected Option",
                selected = false,
                onSelect = { }
            )
        }
    }
}