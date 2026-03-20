package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.mintanable.notethepad.components.TimePickerDialog
import com.mintanable.notethepad.feature_note.R
import java.util.Calendar

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
                TextButton(onClick = { showTimePicker = true }) { Text(stringResource(R.string.next)) }
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