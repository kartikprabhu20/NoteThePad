package com.mintanable.notethepad.feature_note.presentation.notes.components

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.mintanable.notethepad.core.model.note.NoteEntity
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.feature_note.presentation.notes.NotesViewModel
@Composable
fun EvenHandler(
    snackBarHostState: SnackbarHostState,
    viewModel: NotesViewModel = hiltViewModel(),
    context: Context = LocalContext.current,
    onPinWidget: (NoteEntity) -> Unit = {}
) {
    val noteDeletedMsg = stringResource(R.string.msg_note_deleted)
    val undoLabel = stringResource(R.string.label_undo)

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            if (event is NotesViewModel.UiEvent.RequestWidgetPin) {
                onPinWidget(event.note.toNote())
            }
            if(event is NotesViewModel.UiEvent.ShowSnackbar){
                val result = snackBarHostState.showSnackbar(
                    message = noteDeletedMsg,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    event.onAction?.invoke()
                }
            }
        }
    }
}