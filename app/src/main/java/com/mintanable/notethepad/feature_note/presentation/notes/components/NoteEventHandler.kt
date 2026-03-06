package com.mintanable.notethepad.feature_note.presentation.notes.components

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.presentation.notes.NotesViewModel
import com.mintanable.notethepad.feature_widgets.presentation.utils.SingleNoteWidgetReceiver
import com.mintanable.notethepad.feature_widgets.presentation.utils.SingleNoteWidgetReceiver.Companion.PINNING_ACTION
import com.mintanable.notethepad.feature_widgets.presentation.utils.SingleNoteWidgetReceiver.Companion.PINNING_NOTE_ID

@Composable
fun EvenHandler(
    snackBarHostState: SnackbarHostState,
    viewModel: NotesViewModel = hiltViewModel(),
    context: Context = LocalContext.current
) {
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            if (event is NotesViewModel.UiEvent.RequestWidgetPin) {
                pinSingleNoteWidget(context, event.note.toNote())
            }
            if(event is NotesViewModel.UiEvent.ShowSnackbar){
                val result = snackBarHostState.showSnackbar(
                    message = "Note deleted",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    event.onAction?.invoke()
                }
            }
        }
    }
}

private fun pinSingleNoteWidget(context: Context, note: Note) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val myProvider = ComponentName(context, SingleNoteWidgetReceiver::class.java)

    if (appWidgetManager.isRequestPinAppWidgetSupported) {
        val successCallback = Intent(context, SingleNoteWidgetReceiver::class.java).apply {
            putExtra(PINNING_NOTE_ID, note.id)
            action = PINNING_ACTION
        }

        val successPendingIntent = PendingIntent.getBroadcast(
            context,
            note.id?.toInt() ?: 0,
            successCallback,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        appWidgetManager.requestPinAppWidget(myProvider, null, successPendingIntent)
    } else {
        Toast.makeText(context, "Pinned widgets are not supported on this launcher", Toast.LENGTH_SHORT).show()
    }
}