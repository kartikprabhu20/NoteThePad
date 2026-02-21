package com.mintanable.notethepad.feature_settings.presentation.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mintanable.notethepad.feature_settings.domain.model.BackupFrequency
import com.mintanable.notethepad.ui.theme.NoteThePadTheme

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
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewRadioButtonsAlertDialog(){
    NoteThePadTheme {
        RadioButtonsAlertDialog(BackupFrequency.OFF,BackupFrequency.entries,{},{})
    }
}