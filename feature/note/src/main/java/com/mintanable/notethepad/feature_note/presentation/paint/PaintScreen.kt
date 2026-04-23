package com.mintanable.notethepad.feature_note.presentation.paint

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews
import kotlinx.coroutines.launch

enum class PaintSheetMode { NONE, BRUSH, HIGHLIGHTER, ERASER }

const val PAINT_RESULT_KEY = "paintResult"
const val PAINT_OLD_PATH_KEY = "paintOldPath"

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun PaintScreen(
    navController: NavController,
    attachmentPath: String?,
    viewModel: PaintViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    PaintScreenContent(
        attachmentPath = attachmentPath,
        onSaveAndExit = { bitmap, isDirty ->
            if (attachmentPath.isNullOrBlank() && !isDirty) {
                navController.popBackStack()
                return@PaintScreenContent
            }
            if (bitmap == null) {
                navController.popBackStack()
                return@PaintScreenContent
            }
            scope.launch {
                val savedPath = viewModel.saveBitmap(bitmap)
                if (savedPath != null) {
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set(PAINT_OLD_PATH_KEY, attachmentPath)
                        set(PAINT_RESULT_KEY, savedPath)
                    }
                }
                navController.popBackStack()
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaintScreenContent(
    attachmentPath: String?,
    onSaveAndExit: (Bitmap?, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val canvasColor = android.graphics.Color.WHITE
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var androidCanvas by remember { mutableStateOf<Canvas?>(null) }
    var version by remember { mutableIntStateOf(0) }
    var activeTool by remember { mutableStateOf(PaintTool.BRUSH) }
    var isDirty by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var sheetMode by remember { mutableStateOf(PaintSheetMode.NONE) }
    var showClearDialog by remember { mutableStateOf(false) }

    var selectedBrushColor by remember { mutableIntStateOf(NoteColors.colorPairs[0].dark.toArgb()) }
    var brushSizeDp by remember { mutableStateOf(4.dp) }
    val brushStrokeWidthPx = with(density) { brushSizeDp.toPx() }

    var selectedHighlightColor by remember { mutableIntStateOf(NoteColors.colorPairs[5].dark.toArgb()) }
    var highlighterSizeDp by remember { mutableStateOf(16.dp) }
    val highlighterStrokeWidthPx = with(density) { highlighterSizeDp.toPx() }

    var eraserSizeDp by remember { mutableStateOf(4.dp) }
    val eraserStrokeWidthPx = with(density) { eraserSizeDp.toPx() }

    val brushPaint = remember(selectedBrushColor, brushStrokeWidthPx) {
        Paint().apply {
            color = selectedBrushColor
            style = Paint.Style.STROKE
            strokeWidth = brushStrokeWidthPx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
    }
    val eraserPaint = remember(eraserStrokeWidthPx) {
        Paint().apply {
            color = canvasColor
            style = Paint.Style.STROKE
            strokeWidth = eraserStrokeWidthPx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
    }
    val highlighterPaint = remember(selectedHighlightColor, highlighterStrokeWidthPx) {
        Paint().apply {
            color = selectedHighlightColor
            style = Paint.Style.STROKE
            strokeWidth = highlighterStrokeWidthPx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            alpha = 128
            blendMode = BlendMode.MULTIPLY
        }
    }

    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0 && bitmap == null) {
            val newBitmap = createBitmap(canvasSize.width, canvasSize.height)
            val newCanvas = Canvas(newBitmap)
            if (!attachmentPath.isNullOrBlank()) {
                val decoded = BitmapFactory.decodeFile(attachmentPath)
                if (decoded != null) {
                    val srcRect = Rect(0, 0, decoded.width, decoded.height)
                    val dstRect = Rect(0, 0, canvasSize.width, canvasSize.height)
                    newCanvas.drawBitmap(decoded, srcRect, dstRect, null)
                    decoded.recycle()
                } else {
                    newCanvas.drawColor(canvasColor)
                }
            } else {
                newCanvas.drawColor(canvasColor)
            }
            bitmap = newBitmap
            androidCanvas = newCanvas
            version++
        }
    }

    fun handleBack() {
        if (isSaving) return
        isSaving = true
        onSaveAndExit(bitmap, isDirty)
    }

    fun clearCanvas() {
        val canvas = androidCanvas ?: return
        canvas.drawColor(canvasColor)
        version++
        isDirty = true
    }

    BackHandler(enabled = !isSaving) { handleBack() }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            PaintBar(
                activeTool = activeTool,
                onToolClick = { tool ->
                    if (activeTool == tool) {
                        sheetMode = when (tool) {
                            PaintTool.BRUSH -> PaintSheetMode.BRUSH
                            PaintTool.ERASER -> PaintSheetMode.ERASER
                            PaintTool.HIGHLIGHTER -> PaintSheetMode.HIGHLIGHTER
                        }
                    } else {
                        activeTool = tool
                    }
                }
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
            var currentPath by remember { mutableStateOf<Path?>(null) }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(brushPaint, eraserPaint, highlighterPaint, activeTool) {
                        detectDragGestures(
                            onDragStart = { start ->
                                isDirty = true
                                currentPath = Path().apply {
                                    moveTo(start.x, start.y)
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPath?.lineTo(change.position.x, change.position.y)
                                version++
                            },
                            onDragEnd = {
                                val path = currentPath ?: return@detectDragGestures
                                val canvas = androidCanvas ?: return@detectDragGestures
                                val paint = when (activeTool) {
                                    PaintTool.ERASER -> eraserPaint
                                    PaintTool.HIGHLIGHTER -> highlighterPaint
                                    else -> brushPaint
                                }
                                canvas.drawPath(path, paint)
                                currentPath = null
                                version++
                            },
                            onDragCancel = {
                                currentPath = null
                                version++
                            }
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

                currentPath?.let { path ->
                    val paint = when (activeTool) {
                        PaintTool.ERASER -> eraserPaint
                        PaintTool.HIGHLIGHTER -> highlighterPaint
                        else -> brushPaint
                    }
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawPath(path, paint)
                    }
                }
            }
        }

        if (sheetMode != PaintSheetMode.NONE) {
            ModalBottomSheet(
                onDismissRequest = { sheetMode = PaintSheetMode.NONE },
                sheetState = sheetState
            ) {
                when (sheetMode) {
                    PaintSheetMode.BRUSH -> PaintBrushOptionsSheetContent(
                        selectedColor = selectedBrushColor,
                        onColorClick = { selectedBrushColor = it },
                        selectedSizeDp = brushSizeDp,
                        onSizeClick = { brushSizeDp = it }
                    )
                    PaintSheetMode.HIGHLIGHTER -> PaintBrushOptionsSheetContent(
                        selectedColor = selectedHighlightColor,
                        onColorClick = { selectedHighlightColor = it },
                        selectedSizeDp = highlighterSizeDp,
                        onSizeClick = { highlighterSizeDp = it }
                    )
                    PaintSheetMode.ERASER -> PaintEraserOptionsSheetContent(
                        selectedSizeDp = eraserSizeDp,
                        onSizeClick = { eraserSizeDp = it },
                        onClearClick = { showClearDialog = true }
                    )
                    PaintSheetMode.NONE -> Unit
                }
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text(stringResource(R.string.paint_clear_canvas_dialog_title)) },
                text = { Text(stringResource(R.string.paint_clear_canvas_dialog_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        clearCanvas()
                        showClearDialog = false
                        sheetMode = PaintSheetMode.NONE
                    }) {
                        Text(stringResource(R.string.btn_clear))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@ThemePreviews
@Composable
fun PaintScreenPreview() {
    NoteThePadTheme {
        PaintScreenContent(
            attachmentPath = null,
            onSaveAndExit = { _, _ -> }
        )
    }
}
