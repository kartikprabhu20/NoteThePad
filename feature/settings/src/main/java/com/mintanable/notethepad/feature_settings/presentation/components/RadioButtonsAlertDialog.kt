package com.mintanable.notethepad.feature_settings.presentation.components

import android.content.res.Configuration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mintanable.notethepad.core.model.settings.BackupFrequency
import com.mintanable.notethepad.theme.NoteThePadTheme

@Composable
fun <T : Enum<T>> RadioButtonsAlertDialog(
    currentInterval: T,
    entries: Iterable<T>,
    onDismiss: () -> Unit,
    onConfirm: (T) -> Unit,
){
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup Frequency") },
        text = {
            SettingRadioGroup(
                selectedOption = currentInterval,
                onOptionSelected = {
                    onConfirm(it)
                },
                entries = entries
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewRadioButtonsAlertDialog(){
    NoteThePadTheme {
        RadioButtonsAlertDialog(BackupFrequency.OFF,BackupFrequency.entries,{},{})
    }
}