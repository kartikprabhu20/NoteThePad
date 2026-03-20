package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.core.model.note.NoteColors
import com.mintanable.notethepad.feature_note.presentation.notes.BottomSheetType
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews
import kotlinx.coroutines.delay

@Composable
fun NoteBottomAppBar(
    utilityButtons: List<Triple<ImageVector, BottomSheetType, String>>,
    modifier: Modifier = Modifier,
    onActionClick: (BottomSheetType) -> Unit,
    onSaveClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    with(sharedTransitionScope) {
        var isVisible by remember { mutableStateOf(false) }
        val wiggleAnim = remember { Animatable(0f) }

        val transition = animatedVisibilityScope.transition
        val barAlpha by transition.animateFloat(
            label = "BarAlpha",
            transitionSpec = {
                if (targetState == EnterExitState.Visible) {
                    tween(300)
                } else {
                    tween(50)
                }
            }
        ) { state ->
            if (state == EnterExitState.Visible) 1f else 0f
        }

        LaunchedEffect(Unit) {
            isVisible = true
            delay(400)
            wiggleAnim.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 500
                    0f at 0
                    -20f at 100 // Rotate left
                    20f at 200  // Rotate right
                    -10f at 300  // Rotate left (smaller)
                    10f at 400   // Rotate right (smaller)
                    0f at 500   // Back to center
                }
            )
        }

        BottomAppBar(
//            modifier = modifier
//                .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 5f)
//                .graphicsLayer {
//                    alpha = barAlpha
//                    compositingStrategy = CompositingStrategy.Offscreen
//                },
            modifier = modifier
                .let {
                    if (transition.isRunning) {
                        it.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 5f)
                    } else it
                }
                .graphicsLayer {
                    alpha = barAlpha
                    compositingStrategy = CompositingStrategy.Offscreen
                },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
            tonalElevation = 0.dp,
            actions = {

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(300)) + expandHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        expandFrom = Alignment.End
                    ),
                    exit = fadeOut()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {


                        utilityButtons.forEachIndexed { _, (icon, type, label) ->
                            SmallFloatingActionButton(
                                onClick = { onActionClick(type) },
                                shape = RoundedCornerShape(32.dp),
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = wiggleAnim.value
                                }
                            ) {
                                Icon(icon, contentDescription = label)
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    modifier = Modifier.graphicsLayer {
                        rotationZ = wiggleAnim.value
                    },
                    onClick = onSaveClick,
                    containerColor = BottomAppBarDefaults.bottomAppBarFabColor
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save Note")
                }
            }
        )
    }
}


@ThemePreviews
@Composable
fun PreviewBottomAppBar(){
    NoteThePadTheme {

        SharedTransitionLayout {
            AnimatedContent(targetState = true, label = "preview") { isVisible ->
                if (isVisible) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            contentWindowInsets = WindowInsets.systemBars,
                            containerColor = Color.Transparent,
                            bottomBar = {
                                NoteBottomAppBar(
                                    utilityButtons = listOf(
                                        Triple(
                                            Icons.Default.AttachFile,
                                            BottomSheetType.ATTACH,
                                            "Attach"
                                        ),
                                        Triple(
                                            Icons.Default.CheckBox,
                                            BottomSheetType.CHECKBOX,
                                            "CheckBox"
                                        ),
                                        Triple(
                                            Icons.Default.NotificationAdd,
                                            BottomSheetType.REMINDER,
                                            "Reminders"
                                        ),
                                        Triple(
                                            Icons.Default.MoreHoriz,
                                            BottomSheetType.MORE_SETTINGS,
                                            "Settings"
                                        )
                                    ),
                                    onActionClick = { },
                                    onSaveClick = { },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@AnimatedContent
                                )
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NoteColors.colors[0])
                        ) { paddingValue ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(NoteColors.colors[1])
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    contentPadding = paddingValue,
                                ) {
                                    items(20) { index ->
                                        Text(
                                            "Note Content $index",
                                            Modifier.padding(16.dp).fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}