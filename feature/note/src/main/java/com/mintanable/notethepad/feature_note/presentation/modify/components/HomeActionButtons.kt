package com.mintanable.notethepad.feature_note.presentation.modify.components

import android.content.res.Configuration
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.theme.NoteThePadTheme
import kotlinx.coroutines.delay

@Composable
fun HomeActionButtons(
    modifier: Modifier = Modifier,
    isAiAssitantSupported: Boolean = false,
    onAssitantClicked: () -> Unit,
    mainFab: @Composable () -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (isAiAssitantSupported) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = fadeOut()
            ) {

                MagicButton(
                    isVisible = true,
                    shape = RoundedCornerShape(16.dp),
                    onButtonClicked = onAssitantClicked
                )
            }
        }

        mainFab()
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PreviewHomeActionButtons() {
    NoteThePadTheme {
        HomeActionButtons(
            isAiAssitantSupported = true,
            modifier = Modifier,
            onAssitantClicked = {},
            mainFab = {
                FloatingActionButton(
                    onClick = {},
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.content_description_add_note)
                    )
                }
            }
        )
    }
}