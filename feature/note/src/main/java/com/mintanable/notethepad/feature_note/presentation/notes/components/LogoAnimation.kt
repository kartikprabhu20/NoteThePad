package com.mintanable.notethepad.feature_note.presentation.notes.components

import android.graphics.Bitmap
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_note.R
import kotlinx.coroutines.isActive

@Composable
fun LogoRevealAnimation(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnComplete = rememberUpdatedState(onAnimationComplete)
    val clipFraction = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        clipFraction.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = EaseOut)
        )
        if (isActive) {
            currentOnComplete.value()
        }
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val logoBitmap = remember {
        val heightPx = with(density) { 36.dp.toPx() }.toInt()
        val drawable = AppCompatResources.getDrawable(context, R.drawable.notethepad_logo)!!
        val aspectRatio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
        val widthPx = (heightPx * aspectRatio).toInt()
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, widthPx, heightPx)
        drawable.draw(canvas)
        bitmap.asImageBitmap()
    }
    val bitmapAspectRatio = logoBitmap.width.toFloat() / logoBitmap.height

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                currentOnComplete.value()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .height(36.dp)
                .aspectRatio(bitmapAspectRatio)
        ) {
            clipRect(right = size.width * clipFraction.value) {
                drawImage(
                    image = logoBitmap,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                )
            }
        }
    }
}
