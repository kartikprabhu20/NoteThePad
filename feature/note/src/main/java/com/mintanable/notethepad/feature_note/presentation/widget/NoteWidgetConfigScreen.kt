package com.mintanable.notethepad.feature_note.presentation.widget

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mintanable.notethepad.core.model.DetailedNote
import com.mintanable.notethepad.feature_note.presentation.notes.NotesViewModel
import com.mintanable.notethepad.feature_note.presentation.notes.components.StaggeredNotesList

@Composable
fun NoteWidgetConfigScreen(
    notesViewModel: NotesViewModel = hiltViewModel(),
    onNoteClicked: (DetailedNote) -> Unit
) {
    val state by notesViewModel.state.collectAsStateWithLifecycle()
    val isGridView by notesViewModel.isGridViewEnabled.collectAsStateWithLifecycle()

    SharedTransitionLayout {

        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) { contentPadding ->
            AnimatedContent(targetState = true, label = "preview") { isVisible ->
                if (isVisible) {

                    Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {


                        StaggeredNotesList(
                            notes = state.notes,
                            isGridView = isGridView,
                            enableDeletion = false,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent,
                            onNoteClicked = onNoteClicked,
                            onDeleteClicked = { },
                            onPinClicked = { }
                        )
                    }
                }
            }
        }
    }
}
