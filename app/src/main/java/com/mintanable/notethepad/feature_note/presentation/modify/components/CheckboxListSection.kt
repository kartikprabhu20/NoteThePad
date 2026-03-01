package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mintanable.notethepad.feature_note.domain.model.CheckboxItem
import com.mintanable.notethepad.feature_note.domain.model.NoteColors
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import com.mintanable.notethepad.ui.theme.ThemePreviews
import java.util.UUID

fun LazyListScope.checkboxListSection(
    activeDragCheckIndex: String?,
    activeDragUnCheckIndex: String?,
    items: List<CheckboxItem>,
    onEnterPressed: () -> Unit,
    onItemChanged: (CheckboxItem) -> Unit,
    onListOrderUpdated: (List<CheckboxItem>) -> Unit,
    onDragStateChangedChecked: (String?) -> Unit,
    onDragStateChangedUnChecked: (String?) -> Unit
) {

    val uncheckedList = items.filter { !it.isChecked }
    val checkedList = items.filter { it.isChecked }

    checkboxGroup(
        activeDragIndex = activeDragCheckIndex,
        items = uncheckedList,
        isDoneGroup = false,
        onMove = { from, to ->
            val newUncheckedList = uncheckedList.toMutableList().apply { add(to, removeAt(from)) }
            onListOrderUpdated(newUncheckedList + checkedList)
        },
        onEnterPressed = onEnterPressed,
        onItemChanged = onItemChanged,
        onDeletePressed = { index ->
            val newUncheckedList = uncheckedList.toMutableList().apply { removeAt(index) }
            onListOrderUpdated(newUncheckedList + checkedList)
        },
        onDragStateChanged = onDragStateChangedChecked
        
    )

    if (uncheckedList.isNotEmpty() && checkedList.isNotEmpty()) {
        item(key = "divider") {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
        }
    }
    checkboxGroup(
        activeDragIndex = activeDragUnCheckIndex,
        items = checkedList,
        isDoneGroup = true,
        onMove = { from, to ->
            val newCheckedList = checkedList.toMutableList().apply { add(to, removeAt(from)) }
            onListOrderUpdated(uncheckedList + newCheckedList)
        },
        onItemChanged = onItemChanged,
        onDeletePressed = { index ->
            val newCheckedList = checkedList.toMutableList().apply { removeAt(index) }
            onListOrderUpdated(uncheckedList + newCheckedList)
        },
        onEnterPressed = {},
        onDragStateChanged = onDragStateChangedUnChecked
    )
}

@Composable
fun CheckboxRow(
    item: CheckboxItem,
    isHeld: Boolean,
    modifier: Modifier = Modifier,
    dragModifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(),
    isSingleLine: Boolean = false,
    onEnterPressed:() -> Unit,
    onItemChanged: (CheckboxItem) -> Unit,
    onDeletePressed: (String) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(modifier = dragModifier
            .padding(start = 8.dp)
            .padding(top = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Drag to reorder",
                tint = Color.Black,
                modifier = Modifier
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
                .padding(top = 14.dp, bottom = 8.dp)
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

fun LazyListScope.checkboxGroup(
    activeDragIndex: String?,
    items: List<CheckboxItem>,
    isDoneGroup: Boolean,
    onMove: (Int, Int) -> Unit,
    onEnterPressed: () -> Unit,
    onItemChanged: (CheckboxItem) -> Unit,
    onDeletePressed: (Int) -> Unit,
    onDragStateChanged: (String?) -> Unit
) {
    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
        
        val isHeld = item.id == activeDragIndex
        CheckboxRow(
            item = item,
            isHeld = isHeld,
            textStyle = TextStyle(
                textDecoration = if (isDoneGroup) TextDecoration.LineThrough else TextDecoration.None,
                color = if (isDoneGroup) Color.Gray else Color.Unspecified
            ),
            modifier = Modifier
                .animateItem()
                .zIndex(if (isHeld) 1f else 0f)
                .graphicsLayer {
                    shadowElevation = if (isHeld) 10f else 0f
                },
            dragModifier = Modifier.dragHandler(
                    item = item,
                    items = items,
                    onMove = onMove,
                    onDragStateChanged = onDragStateChanged
                ),
            onEnterPressed = onEnterPressed,
            onItemChanged = onItemChanged,
            onDeletePressed = { onDeletePressed(index) }
        )
    }
}

@Composable
fun Modifier.dragHandler(
    item: CheckboxItem,
    items: List<CheckboxItem>,
    onMove: (Int, Int) -> Unit,
    onDragStateChanged: (String?) -> Unit
): Modifier {
    val currentItems by rememberUpdatedState(items)
    val currentOnMove by rememberUpdatedState(onMove)

    return this.pointerInput(item.id) {
        var draggedDistance = 0f

        detectDragGestures(
            onDragStart = {
                onDragStateChanged(item.id)
                draggedDistance = 0f
            },
            onDrag = { change, dragAmount ->
                change.consume()
                draggedDistance += dragAmount.y
                val itemHeightPx = 56.dp.toPx() // approximate your row height

                val listSnapshot = currentItems
                val currentIndex = listSnapshot.indexOfFirst { it.id == item.id }

                if (currentIndex != -1) {
                    val moveBy = (draggedDistance / itemHeightPx).toInt()
                    if (moveBy != 0) {
                        val targetIndex = (currentIndex + moveBy).coerceIn(0, listSnapshot.size - 1)
                        if (targetIndex != currentIndex) {
                            currentOnMove(currentIndex, targetIndex)
                            draggedDistance -= moveBy * itemHeightPx // subtract only what was consumed
                        }
                    }
                }
            },
            onDragCancel = { onDragStateChanged(null) },
            onDragEnd = { onDragStateChanged(null) }
        )
    }
}


@ThemePreviews
@Composable
fun PreviewCheckboxListSection(){
    NoteThePadTheme {
        Box(modifier = Modifier.background(NoteColors.colors.get(0))) {

            val id = UUID.randomUUID().toString()
            LazyColumn {
                checkboxListSection(
                    items = listOf(
                        CheckboxItem(
                            text = "test1 i hope this line extends beyond the limits, so i can test how multiple lines are displayed in the row. But unfortunately seems like i need to make some changes",
                            isChecked = true
                        ),
                        CheckboxItem(text = "test2", isChecked = false),
                        CheckboxItem(
                            text = "test3 hope this line extends beyond the limits, so i can test how multiple lines are displayed in the row. But unfortunately seems like i need to make some changes",
                            isChecked = false
                        ),
                        CheckboxItem(text = "test4", isChecked = true),
                        CheckboxItem(
                            id = id,
                            text = "test1 i hope this line extends beyond the limits, so i can test how multiple lines are displayed in the row. But unfortunately seems like i need to make some changes",
                            isChecked = true
                        ),
                        CheckboxItem(text = "test2", isChecked = false),
                        CheckboxItem(
                            text = "test3 hope this line extends beyond the limits, so i can test how multiple lines are displayed in the row. But unfortunately seems like i need to make some changes",
                            isChecked = false
                        ),
                        CheckboxItem(text = "test4", isChecked = true)

                    ),
                    onEnterPressed = {},
                    onItemChanged = {},
                    onListOrderUpdated = {},
                    activeDragCheckIndex = id,
                    activeDragUnCheckIndex = null,
                    onDragStateChangedChecked = {},
                    onDragStateChangedUnChecked = { },
                )
            }
        }
    }
}
