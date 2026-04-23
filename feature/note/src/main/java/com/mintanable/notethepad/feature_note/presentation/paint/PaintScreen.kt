package com.mintanable.notethepad.feature_note.presentation.paint

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch

const val PAINT_RESULT_KEY = "paintResult"

@Composable
fun PaintScreen(
    navController: NavController,
    attachmentPath: String?,
    viewModel: PaintViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var androidCanvas by remember { mutableStateOf<android.graphics.Canvas?>(null) }
    var version by remember { mutableIntStateOf(0) }
    var activeTool by remember { mutableStateOf(PaintTool.BRUSH) }
    var isDirty by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val strokeWidthPx = with(density) { 4.dp.toPx() }

    val brushPaint = remember(strokeWidthPx) {
        Paint().apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
    }
    val eraserPaint = remember(strokeWidthPx) {
        Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx * 3f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
    }

    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0 && bitmap == null) {
            val newBitmap = Bitmap.createBitmap(
                canvasSize.width,
                canvasSize.height,
                Bitmap.Config.ARGB_8888
            )
            val newCanvas = android.graphics.Canvas(newBitmap)
            if (!attachmentPath.isNullOrBlank()) {
                val decoded = BitmapFactory.decodeFile(attachmentPath)
                if (decoded != null) {
                    val srcRect = android.graphics.Rect(0, 0, decoded.width, decoded.height)
                    val dstRect = android.graphics.Rect(0, 0, canvasSize.width, canvasSize.height)
                    newCanvas.drawBitmap(decoded, srcRect, dstRect, null)
                    decoded.recycle()
                }
            }
            bitmap = newBitmap
            androidCanvas = newCanvas
            version++
        }
    }

    fun saveAndExit() {
        if (isSaving) return
        val currentBitmap = bitmap
        if (attachmentPath.isNullOrBlank() && !isDirty) {
            navController.popBackStack()
            return
        }
        if (currentBitmap == null) {
            navController.popBackStack()
            return
        }
        isSaving = true
        scope.launch {
            val savedPath = viewModel.saveBitmap(currentBitmap, attachmentPath)
            if (savedPath != null) {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(PAINT_RESULT_KEY, savedPath)
            }
            navController.popBackStack()
        }
    }

    BackHandler(enabled = !isSaving) { saveAndExit() }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            PaintBar(
                activeTool = activeTool,
                onToolClick = { activeTool = it }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .onSizeChanged { canvasSize = it }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var last: Offset? = null
                        detectDragGestures(
                            onDragStart = { start ->
                                val canvas = androidCanvas ?: return@detectDragGestures
                                last = start
                                isDirty = true
                                val paint = if (activeTool == PaintTool.ERASER) eraserPaint else brushPaint
                                canvas.drawPoint(start.x, start.y, paint)
                                version++
                            },
                            onDrag = { change, _ ->
                                val canvas = androidCanvas ?: return@detectDragGestures
                                change.consume()
                                val prev = last ?: change.position
                                val curr = change.position
                                val paint = if (activeTool == PaintTool.ERASER) eraserPaint else brushPaint
                                canvas.drawLine(prev.x, prev.y, curr.x, curr.y, paint)
                                last = curr
                                version++
                            },
                            onDragEnd = { last = null },
                            onDragCancel = { last = null }
                        )
                    }
            ) {
                @Suppress("UNUSED_EXPRESSION") version
                val bmp = bitmap
                if (bmp != null) {
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawBitmap(bmp, 0f, 0f, null)
                    }
                }
            }
        }
    }
}
