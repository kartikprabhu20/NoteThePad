package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.util.Calendar
import com.mintanable.notethepad.feature_settings.presentation.components.TimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateAndTimePicker(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val dateState = rememberDatePickerState()
    val timeState = rememberTimePickerState()
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    if (!showTimePicker) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { showTimePicker = true }) { Text("Next") }
            }
        ) {
            DatePicker(state = dateState)
        }
    } else {
        TimePickerDialog(
            onDismiss = {
                showTimePicker = false
            },
            onConfirm = { hour, minute ->
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = dateState.selectedDateMillis ?: System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                onConfirm(calendar.timeInMillis)

            }
        )
    }
}