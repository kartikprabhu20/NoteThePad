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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.mintanable.notethepad.feature_note.R

@Composable
fun MagicButton(
    title: String? = null,
    imageVector: ImageVector? = Icons.Default.AutoAwesome,
    painter: Painter? = null,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    showMagicBorder: Boolean = true,
    onButtonClicked: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val transition = animatedVisibilityScope?.transition
    val isExiting = transition?.targetState?.let { it != EnterExitState.Visible } ?: false

    val screenAlpha by transition?.animateFloat(
        label = "MagicButtonScreenAlpha",
        transitionSpec = { if (isExiting) tween(300) else tween(50) }
    ) { state ->
        if (state == EnterExitState.Visible) 1f else 0f
    } ?: rememberUpdatedState(1f)

    val shimmerOffset = remember { Animatable(0f) }
    LaunchedEffect (Unit) {
        shimmerOffset.animateTo(
            targetValue = 2000f,
            animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing)
        )
    }

    val isIconOnly = title.isNullOrBlank()
    val buttonModifier = if (isIconOnly) {
        Modifier.size(42.dp)
    } else {
        Modifier
    }
    val horizontalPadding = if (isIconOnly) 0.dp else 24.dp
    val verticalPadding = if (isIconOnly) 0.dp else 12.dp


    val magicButton = @Composable { contentModifier: Modifier ->
        Surface(
            onClick = onButtonClicked,
            shape = shape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
            tonalElevation = 0.dp,
            modifier = contentModifier
                .then(buttonModifier)
                .let {
                    if (showMagicBorder) it.magicBorder(
                        width = 3.dp,
                        shimmerOffset = shimmerOffset.value,
                        shape = shape
                    ) else it
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding
                )
            ) {
                if (painter != null) {
                    Icon(
                        painter = painter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (imageVector != null) {
                    Icon(
                        imageVector = imageVector,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (!isIconOnly) {
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

    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            AnimatedVisibility(
                visible = isVisible && !isExiting,
                enter = fadeIn() + scaleIn(initialScale = 0.8f) + expandVertically(),
                exit = fadeOut(),
                modifier = modifier
                    .graphicsLayer { alpha = screenAlpha }
                    .let { if (isExiting) it.skipToLookaheadSize() else it }
                    .let {
                        if (transition?.isRunning == true && !isExiting) {
                            it.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 5f)
                        } else it
                    }
            ) {
                magicButton(Modifier)
            }
        }
    } else {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut(),
            modifier = modifier
        ) {
            magicButton(Modifier)
        }
    }
}

@Composable
fun Modifier.magicBorder(
    width: Dp = 2.dp,
    shimmerOffset: Float,
    shape: Shape = CircleShape
): Modifier {

    val magicBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF6200EE), // Deep Purple
            Color(0xFF03DAC6), // Teal
            Color(0xFFFF0266), // Pink
            Color(0xFF6200EE), // Back to Purple
        ),
        start = Offset(shimmerOffset - 500f, 0f),
        end = Offset(shimmerOffset, 200f),
        tileMode = TileMode.Mirror
    )

    return this.border(width = width, brush = magicBrush, shape = shape)
}

@ThemePreviews
@Composable
fun PreviewMagicBorder() {
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

@ThemePreviews
@Composable
fun PreviewMagicBorderIcon() {
    NoteThePadTheme {
        SharedTransitionLayout {
            AnimatedContent(targetState = true, label = "preview") { isVisible ->
                if (isVisible) {
                    Row(
                        modifier = Modifier
                            .background(NoteColors.colors[2])
                            .padding(24.dp)
                    ) {
                        MagicButton(
                            isVisible = true,
                            painter = painterResource(R.drawable.speech_to_text_24px),
                            modifier = Modifier.align(Alignment.CenterVertically),
                            shape = RoundedCornerShape(6.dp),
                            onButtonClicked = {}
                        )
                    }
                }

            }
        }
    }
}