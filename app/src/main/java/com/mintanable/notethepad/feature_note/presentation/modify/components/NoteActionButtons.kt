package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_note.domain.util.BottomSheetType
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import kotlinx.coroutines.delay

@Composable
fun NoteActionButtons(
    modifier: Modifier = Modifier,
    onActionClick: (BottomSheetType) -> Unit,
    onSaveClick: () -> Unit,
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

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.End // Keeps them aligned to the right
    ) {

        val utilityButtons = listOf(
            Triple(Icons.Default.AttachFile, BottomSheetType.ATTACH, "Attach"),
            Triple(Icons.Default.NotificationAdd, BottomSheetType.REMINDER, "Reminders"),
            Triple(Icons.Default.MoreHoriz, BottomSheetType.MORE_SETTINGS, "Settings")
        )

        utilityButtons.forEachIndexed { index, (icon, type, label) ->
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight * (utilityButtons.size - index) },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                ) + fadeIn(),
                exit = fadeOut()
            ) {
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

        FloatingActionButton(
            onClick = onSaveClick,
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = "Save Note")
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun previewNoteActionButtons(){
    NoteThePadTheme {
        NoteActionButtons(
            onActionClick = {},
            onSaveClick = {}
        )
    }
}