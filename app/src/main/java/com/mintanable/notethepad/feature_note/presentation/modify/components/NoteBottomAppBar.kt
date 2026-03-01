package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_note.domain.model.NoteColors
import com.mintanable.notethepad.feature_note.presentation.notes.BottomSheetType
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import com.mintanable.notethepad.ui.theme.ThemePreviews
import kotlinx.coroutines.delay

@Composable
fun NoteBottomAppBar(
    modifier: Modifier = Modifier,
    onActionClick:  (BottomSheetType) -> Unit,
    onSaveClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val wiggleAnim = remember { Animatable(0f) }

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
        modifier = modifier,
        containerColor = Color.Black.copy(alpha = 0.75f),
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
                    val utilityButtons = listOf(
                        Triple(Icons.Default.AttachFile, BottomSheetType.ATTACH, "Attach"),
                        Triple(Icons.Default.CheckBox, BottomSheetType.CHECKBOX, "CheckBox") ,
                        Triple(Icons.Default.NotificationAdd, BottomSheetType.REMINDER, "Reminders"),
                        Triple(Icons.Default.MoreHoriz, BottomSheetType.MORE_SETTINGS, "Settings")
                    )

                    utilityButtons.forEachIndexed { index, (icon, type, label) ->
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


@ThemePreviews
@Composable
fun PreviewBottomAppBar(){
    NoteThePadTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                contentWindowInsets = WindowInsets.systemBars,
                containerColor = Color.Transparent,
                bottomBar = {
                    NoteBottomAppBar(
                        onActionClick = { },
                        onSaveClick = { }
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(NoteColors.colors.get(0))
            ) { paddingValue ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NoteColors.colors.get(0))
                ){
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