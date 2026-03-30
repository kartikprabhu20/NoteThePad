package com.mintanable.notethepad.feature_settings.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.components.drawNoteShape
import com.mintanable.notethepad.core.model.settings.NoteShape
import com.mintanable.notethepad.feature_settings.R

@Composable
fun NoteShapePickerDialog(
    currentShape: NoteShape,
    onShapeSelected: (NoteShape) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_select_note_shape_title)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(NoteShape.entries.toList()) { shape ->
                    val isSelected = shape == currentShape
                    NoteShapePreviewItem(
                        shape = shape,
                        isSelected = isSelected,
                        onClick = {
                            onShapeSelected(shape)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
private fun NoteShapePreviewItem(
    shape: NoteShape,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val sampleColor = NoteColors.colors.random().toArgb()
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isSelected) 3.dp else 0.dp

    Column(
        modifier = Modifier
            .padding(4.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawNoteShape(shape, sampleColor)
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Title",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Sample content",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = noteShapeDisplayName(shape),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
    }
}

@Composable
fun noteShapeDisplayName(shape: NoteShape): String = when (shape) {
    NoteShape.DEFAULT -> stringResource(R.string.note_shape_default)
    NoteShape.STICKY_CLIPPED -> stringResource(R.string.note_shape_sticky_clipped)
    NoteShape.PERFORATED_PAPER -> stringResource(R.string.note_shape_perforated_paper)
    NoteShape.FROSTED_GLASS -> stringResource(R.string.note_shape_frosted_glass)
    NoteShape.TAPED_NOTE -> stringResource(R.string.note_shape_taped_note)
    NoteShape.BLUEPRINT_GRID -> stringResource(R.string.note_shape_blueprint_grid)
    NoteShape.SCALLOPED_EDGE -> stringResource(R.string.note_shape_scalloped_edge)
    NoteShape.STICKY_CORNER_CURL -> stringResource(R.string.note_shape_sticky_corner_curl)
    NoteShape.CRUSHED_PAPER -> stringResource(R.string.note_shape_crushed_paper)
    NoteShape.WASHI_TAPE_TOP -> stringResource(R.string.note_shape_washi_tape_top)
    NoteShape.STAPLED_CORNER -> stringResource(R.string.note_shape_stapled_corner)
    NoteShape.PINNED_NOTE -> stringResource(R.string.note_shape_pinned_note)
    NoteShape.SUN_BLEACHED_FADE -> stringResource(R.string.note_shape_sun_bleached_fade)
}
