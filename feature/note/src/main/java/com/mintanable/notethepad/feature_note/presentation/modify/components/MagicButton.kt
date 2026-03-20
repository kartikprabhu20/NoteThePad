package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.core.model.note.NoteColors
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews

@Composable
fun MagicButton(
    title: String,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    onButtonClicked: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {

    val transition = animatedVisibilityScope.transition
    val isExiting = transition.targetState != EnterExitState.Visible
    val screenAlpha by transition.animateFloat(
        label = "MagicButtonScreenAlpha",
        transitionSpec = {
            if (isExiting) {
                tween(300)
            } else {
                tween(50)
            }
        }
    ) { state ->
        if (state == EnterExitState.Visible) 1f else 0f
    }

    with(sharedTransitionScope) {
        AnimatedVisibility(
            visible = isVisible && !isExiting,
            enter = fadeIn() + scaleIn(initialScale = 0.8f) + expandVertically(),
            exit = fadeOut(),
            modifier = modifier
                .graphicsLayer { alpha = screenAlpha }
                .let { if (isExiting) it.skipToLookaheadSize() else it }
                .let {
                    if (transition.isRunning && !isExiting) {
                        it.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 5f)
                    } else it
                }
        ) {
            Surface(
                onClick = onButtonClicked,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .magicBorder(width = 2.dp, shape = CircleShape)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        horizontal = 24.dp,
                        vertical = 12.dp
                    ) // Custom padding
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}


@Composable
fun Modifier.magicBorder(
    width: Dp = 2.dp,
    shape: RoundedCornerShape = CircleShape
): Modifier {

    val shimmerOffset = remember { Animatable(0f) }
    LaunchedEffect (Unit) {
        shimmerOffset.animateTo(
            targetValue = 2000f,
            animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing)
        )
    }

    val magicBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF6200EE), // Deep Purple
            Color(0xFF03DAC6), // Teal
            Color(0xFFFF0266), // Pink
            Color(0xFF6200EE), // Back to Purple
        ),
        start = Offset(shimmerOffset.value - 500f, 0f),
        end = Offset(shimmerOffset.value, 200f),
        tileMode = TileMode.Mirror
    )

    return this.border(width = width, brush = magicBrush, shape = shape)
}

@ThemePreviews
@Composable
fun previewMagicBorder() {
    NoteThePadTheme {
        SharedTransitionLayout {
            AnimatedContent(targetState = true, label = "preview") { isVisible ->
                if (isVisible) {
                    Box(
                        modifier = Modifier
                            .background(NoteColors.colors[2])
                            .padding(24.dp)
                    ) {
                        MagicButton(
                            title = "Auto-Tag",
                            isVisible = true,
                            onButtonClicked = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent
                        )
                    }
                }

            }
        }
    }
}