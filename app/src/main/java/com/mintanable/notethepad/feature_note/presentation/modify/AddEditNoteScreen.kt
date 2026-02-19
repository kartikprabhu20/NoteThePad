package com.mintanable.notethepad.feature_note.presentation.modify

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mintanable.notethepad.feature_note.domain.model.NoteColors
import com.mintanable.notethepad.feature_note.presentation.notes.components.TransparentHintTextField
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun AddEditNoteScreen(
    navController: NavController,
    noteId: Int?,
    noteColor: Int,
    viewModel: AddEditNoteViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope
){
    val titleState = viewModel.noteTitle.value
    val contentState = viewModel.noteContent.value
    val snackBarHostState = remember { SnackbarHostState() }

    val noteBackgroundAnimatable = remember{
        Animatable(
            Color(if(noteColor!=-1) noteColor else viewModel.noteColor.value)
        )
    }

    val scope = rememberCoroutineScope()
    LaunchedEffect(key1 = true){
        viewModel.eventFlow.collectLatest { event->
            when(event){
                is AddEditNoteViewModel.UiEvent.ShowSnackbar->{
                    snackBarHostState.showSnackbar(
                        message = event.message
                    )
                }
                is AddEditNoteViewModel.UiEvent.SaveNote->{
                    navController.navigateUp()
                }

                else -> {}
            }
        }
    }

    with(sharedTransitionScope) {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        viewModel.onEvent(AddEditNoteEvent.SaveNote)
                              },
                    containerColor = MaterialTheme.colorScheme.primary
                ){
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save Note")
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(noteBackgroundAnimatable.value)
//                .sharedBounds(
//                    sharedContentState = rememberSharedContentState(
//                        key = if (noteId == -1) "notescreens_fab" else "note-$noteId"
//                    ),
//                    animatedVisibilityScope = animatedVisibilityScope,
//                    enter = fadeIn(tween(200)),
//                    exit = fadeOut(tween(300)),
//                    boundsTransform = { _, _ ->
//                        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
//                    },
//                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
//                )
//             .renderInSharedTransitionScopeOverlay(
//                 zIndexInOverlay = if (animatedVisibilityScope.transition.isRunning) 2f else 0f
//             )
            ,
        ) { paddingValue ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(noteBackgroundAnimatable.value)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = if (noteId == -1) "notescreens_fab" else "note-$noteId"
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(300)),
                        boundsTransform = { _, _ ->
                            spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
                        },
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    )
//                    .renderInSharedTransitionScopeOverlay(
//                        zIndexInOverlay = if (animatedVisibilityScope.transition.isRunning) 2f else 0f
//                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValue)
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NoteColors.colors.forEach { color ->
                            val colorInt = color.toArgb()
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .shadow(15.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = 3.dp,
                                        color = if (viewModel.noteColor.value == colorInt) {
                                            Color.Black
                                        } else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        scope.launch {
                                            noteBackgroundAnimatable.animateTo(
                                                targetValue = Color(colorInt),
                                                animationSpec = tween(
                                                    durationMillis = 500
                                                )
                                            )
                                        }
                                        viewModel.onEvent(AddEditNoteEvent.ChangeColor(colorInt))
                                    }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TransparentHintTextField(
                        text = titleState.text,
                        hint = titleState.hint,
                        onValueChange = {
                            viewModel.onEvent(AddEditNoteEvent.EnteredTitle(it))
                        },
                        onFocusChange = {
                            viewModel.onEvent(AddEditNoteEvent.ChangeTitleFocus(it))
                        },
                        isHintVisible = titleState.isHintVisible,
                        isSingleLine = true,
                        textStyle = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier
                            .sharedBounds(
                                sharedContentState = sharedTransitionScope.rememberSharedContentState(
                                    key = "note-title-${noteId}"
                                ),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    tween()
                                },
                                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                            ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    TransparentHintTextField(
                        text = contentState.text,
                        hint = contentState.hint,
                        onValueChange = {
                            viewModel.onEvent(AddEditNoteEvent.EnteredContent(it))
                        },
                        onFocusChange = {
                            viewModel.onEvent(AddEditNoteEvent.ChangeContentFocus(it))
                        },
                        isHintVisible = contentState.isHintVisible,
                        isSingleLine = false,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxHeight().sharedBounds(
                            sharedContentState = sharedTransitionScope.rememberSharedContentState(
                                key = "note-content-${noteId}"
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                tween()
                            },
                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                        )
                    )
                }
            }
        }
    }
}