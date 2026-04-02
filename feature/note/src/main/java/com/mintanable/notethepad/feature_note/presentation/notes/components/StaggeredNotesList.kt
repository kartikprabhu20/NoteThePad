package com.mintanable.notethepad.feature_note.presentation.notes.components

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.core.model.settings.NoteShape
import com.mintanable.notethepad.database.db.entity.DetailedNote

@Composable
fun StaggeredNotesList(
    notes: List<DetailedNote>,
    noteShape: NoteShape = NoteShape.DEFAULT,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    onNoteClicked: (DetailedNote) -> Unit,
    onDeleteClicked: (DetailedNote) -> Unit,
    onPinClicked: (DetailedNote) -> Unit,
    enableDeletion: Boolean = true,
    isDarkTheme: Boolean = false,
    isSyncEnabled: Boolean = false,
    isSyncing: Boolean = false,
    onRefresh: () -> Unit
) {

    var columnCount by rememberSaveable() { mutableIntStateOf(2) }

    val content = remember(notes,columnCount){
        movableContentOf {
            with(sharedTransitionScope) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                var zoomAccumulator = 1f

                                //Wait for TWO fingers to touch down
                                val firstDown = awaitFirstDown(requireUnconsumed = false)
                                val secondDown =
                                    awaitPointerEvent().changes.firstOrNull { it.id != firstDown.id }

                                if (secondDown != null) {
                                    // Pinch starting!
                                    do {
                                        val event = awaitPointerEvent()
                                        val zoom =
                                            event.calculateZoom() // Relative change since last event

                                        if (zoom != 1f) {
                                            zoomAccumulator *= zoom

                                            // Adjust these thresholds to your liking (Lower = More Sensitive)
                                            val zoomInThreshold = 1.35f
                                            val zoomOutThreshold = 0.85f

                                            when {
                                                zoomAccumulator > zoomInThreshold && columnCount > 1 -> {
                                                    columnCount--
                                                    zoomAccumulator = 1f // Reset after trigger
                                                    event.changes.forEach { it.consume() }
                                                }

                                                zoomAccumulator < zoomOutThreshold && columnCount < 3 -> {
                                                    columnCount++
                                                    zoomAccumulator = 1f // Reset after trigger
                                                    event.changes.forEach { it.consume() }
                                                }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                        }
                ) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(columnCount),
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
        }
    }

    if (isSyncEnabled) {
        PullToRefreshBox(
            isRefreshing = isSyncing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            content()
        }
    } else {
        content()
    }
}