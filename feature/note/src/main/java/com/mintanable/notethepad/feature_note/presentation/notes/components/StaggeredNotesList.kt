package com.mintanable.notethepad.feature_note.presentation.notes.components

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.core.model.settings.NoteShape
import com.mintanable.notethepad.database.db.entity.DetailedNote

@Composable
fun StaggeredNotesList(
    notes: List<DetailedNote>,
    isGridView: Boolean,
    noteShape: NoteShape = NoteShape.DEFAULT,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    onNoteClicked: (DetailedNote) -> Unit,
    onDeleteClicked: (DetailedNote) -> Unit,
    onPinClicked: (DetailedNote) -> Unit,
    enableDeletion: Boolean = true,
    isDarkTheme: Boolean = false,
    isSyncEnabled: Boolean = false
) {

    with(sharedTransitionScope) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(if (isGridView) 2 else 1),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                notes,
                key = { note -> note.id ?: -1 },
                contentType = { "note_item" }
            ) { note ->
                NoteItemUI(
                    note = note,
                    noteShape = noteShape,
                    enableDeleteIcon = enableDeletion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .sharedBounds(
                            sharedContentState = sharedTransitionScope.rememberSharedContentState(
                                key = "note-${note.id}"
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
                            },
                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                        )
                        .clickable { onNoteClicked(note) },
                    onDeleteClick = {
                        onDeleteClicked(note)
                    },
                    onPinClick = {
                        onPinClicked(note)
                    },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    isDarkTheme = isDarkTheme,
                    isSyncEnabled = isSyncEnabled
                )
            }
        }
    }
}