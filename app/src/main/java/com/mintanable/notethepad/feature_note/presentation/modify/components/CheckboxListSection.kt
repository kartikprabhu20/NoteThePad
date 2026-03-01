package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_note.domain.model.CheckboxItem
import com.mintanable.notethepad.feature_note.domain.model.NoteColors
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import com.mintanable.notethepad.ui.theme.ThemePreviews

@Composable
fun CheckboxListSection(
    items: List<CheckboxItem>,
    onEnterPressed: () -> Unit,
    onItemChanged: (CheckboxItem) -> Unit,
    onDeletePressed: (String) -> Unit
 ) {
    val unchecked = items.filter { !it.isChecked }
    val checked = items.filter { it.isChecked }

    val lazyListState = rememberLazyListState()

    LazyColumn(state = lazyListState,
        modifier = Modifier.fillMaxWidth()
    ) {
        items(items = unchecked, key = { it.id }) { item ->
            CheckboxRow(
                item = item,
                textStyle = MaterialTheme.typography.bodyLarge,
                onEnterPressed = onEnterPressed,
                onItemChanged = onItemChanged,
                onDeletePressed = onDeletePressed
            )
        }


        if (checked.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp))
            }
        }

        items(items = checked, key = { it.id }) { item ->
            CheckboxRow(
                item = item,
                textStyle = MaterialTheme.typography.bodyLarge,
                onEnterPressed = onEnterPressed,
                onItemChanged = onItemChanged,
                onDeletePressed = onDeletePressed
            )
        }
    }
}

@Composable
fun CheckboxRow(
    item: CheckboxItem,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(),
    isSingleLine: Boolean = false,
    onEnterPressed:() -> Unit,
    onItemChanged: (CheckboxItem) -> Unit,
    onDeletePressed: (String) -> Unit
) {
    Row(
        modifier=modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ){
        IconButton(
            modifier = Modifier,
            onClick = { onDeletePressed(item.id) }
        ) {
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Drag to reorder",
                tint = Color.Black,
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                // Call a function: onMove(item.id, dragAmount.y)
                            },
                            onDragEnd = { /* Save to DB now */ }
                        )
                    }
            )
        }

        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onItemChanged(item.copy(isChecked = !item.isChecked)) }
        )

        BasicTextField(
            value = item.text,
            onValueChange = { onItemChanged(item.copy(text = it)) },
            singleLine = isSingleLine,
            textStyle = textStyle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .padding(horizontal = 4.dp)
                .weight(1F)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown) {
                        onEnterPressed()
                        true
                    } else {
                        false
                    }
                }
        )

        IconButton(
            modifier = Modifier,
            onClick = { onDeletePressed(item.id) }
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete checkbox",
                tint = Color.Black
            )
        }
    }
}

@Composable
fun ReorderableCheckboxList(
    items: List<CheckboxItem>,
    onMove: (Int, Int) -> Unit,
    onDragEnd: () -> Unit,
    onItemChanged: (CheckboxItem) -> Unit,
    onEnterPressed: () -> Unit,
    onDeletePressed: (String) -> Unit
) {
    val listState = rememberLazyListState()
    var draggedDistance by rememberSaveable { mutableFloatStateOf(0f) }
    var initiallyDraggedElement by rememberSaveable { mutableStateOf<Int?>(null) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            CheckboxRow(
                item = item,
                onItemChanged = onItemChanged,
                onEnterPressed = onEnterPressed,
                onDeletePressed = onDeletePressed,
                modifier = Modifier
                    .animateItem()
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                initiallyDraggedElement = index
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                draggedDistance += dragAmount.y
                                val currentElementIndex = initiallyDraggedElement ?: return@detectDragGesturesAfterLongPress
                                if (draggedDistance > 50f && currentElementIndex < items.size - 1) {
                                    onMove(currentElementIndex, currentElementIndex + 1)
                                    initiallyDraggedElement = currentElementIndex + 1
                                    draggedDistance = 0f
                                } else if (draggedDistance < -50f && currentElementIndex > 0) {
                                    onMove(currentElementIndex, currentElementIndex - 1)
                                    initiallyDraggedElement = currentElementIndex - 1
                                    draggedDistance = 0f
                                }
                            },
                            onDragEnd = {
                                initiallyDraggedElement = null
                                draggedDistance = 0f
                                onDragEnd()
                            },
                            onDragCancel = {
                                initiallyDraggedElement = null
                                draggedDistance = 0f
                            }
                        )
                    }
            )
        }
    }
}


@ThemePreviews
@Composable
fun PreviewCheckboxListSection(){
    NoteThePadTheme {
        Box(modifier = Modifier.background(NoteColors.colors.get(0))) {
            CheckboxListSection(
                items = listOf(
                    CheckboxItem(
                        text = "test1 i hope this line extends beyond the limits, so i can test how multiple lines are displayed in the row. But unfortunately seems like i need to make some changes",
                        isChecked = true
                    ), CheckboxItem(text = "test2", isChecked = false)
                ),
                onEnterPressed = {},
                onItemChanged = {},
                onDeletePressed = {}
            )
        }
    }
}
