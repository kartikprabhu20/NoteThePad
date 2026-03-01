package com.mintanable.notethepad.feature_settings.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int? = null,
    initialMinute: Int? = null,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {

    val now = LocalDateTime.now()
    val hour = now.hour
    val minute = now.minute

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour?: hour,
        initialMinute = initialMinute?: minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(timePickerState.hour, timePickerState.minute)
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewTimePickerDialog(){
    NoteThePadTheme {
        TimePickerDialog(1, 1, {}, {hour,minute-> })
    }
}