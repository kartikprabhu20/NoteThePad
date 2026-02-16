package com.mintanable.notethepad.feature_settings.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_settings.domain.model.ThemeMode
import com.mintanable.notethepad.ui.theme.NoteThePadTheme

@Composable
fun <T : Enum<T>> SettingRadioGroup(
    title: String,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    entries: Iterable<T>
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        entries.forEach { entry ->
            Row(
                Modifier.fillMaxWidth().selectable(
                    selected = (entry == selectedOption),
                    onClick = { onOptionSelected(entry) }
                ).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = (entry == selectedOption), onClick = null)

                Text(text = entry.name, modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSettingRadioGroup(){
    NoteThePadTheme {
        SettingRadioGroup("Theme Mode", ThemeMode.DARK, {}, ThemeMode.entries)
    }
}