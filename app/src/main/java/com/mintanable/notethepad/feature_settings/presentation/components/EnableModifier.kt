package com.mintanable.notethepad.feature_settings.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.enabled(enabled: Boolean): Modifier {
    return this
        .graphicsLayer {
            alpha = if (enabled) 1f else 0.4f
        }
        .then(
            if (enabled) Modifier
            else Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                    }
                }
            }
        )
}