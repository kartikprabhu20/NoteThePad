package com.mintanable.notethepad.feature_note.presentation.notes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.mintanable.notethepad.R
import com.mintanable.notethepad.feature_note.domain.model.CheckboxItem
import com.mintanable.notethepad.feature_note.domain.model.NoteColors
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import com.mintanable.notethepad.ui.theme.ThemePreviews

@Composable
fun SimpleCheckboxList(checklist: List<CheckboxItem>) {
    Column {
        checklist
            .filter { it.text.isNotBlank() }
            .take(10)
            .forEach { item ->
                SimpleCheckbox(item.text, item.isChecked)
            }
    }
}

@Composable
fun SimpleCheckbox(itemText: String, isChecked: Boolean, modifier: Modifier = Modifier) {

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if(isChecked)Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = stringResource(R.string.content_description_checkbox_state),
            tint = if(isChecked) Color.Gray else Color.Black
        )
        Text(
            text = itemText,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
            color = if (isChecked) Color.Gray else Color.Black
        )
    }
}

@ThemePreviews
@Composable
fun PreviewSimpleCheckbox() {
    val items = listOf(
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
            text = "test1 i hope this line extends beyond the limits, so i can test how multiple lines are displayed in the row. But unfortunately seems like i need to make some changes",
            isChecked = true
        ),
        CheckboxItem(text = "test2", isChecked = false),
        CheckboxItem(
            text = "test3 hope this line extends beyond the limits, so i can test how multiple lines are displayed in the row. But unfortunately seems like i need to make some changes",
            isChecked = false
        ),
        CheckboxItem(text = "test4", isChecked = true)
    )

    NoteThePadTheme {
        Box(modifier = Modifier.background(NoteColors.colors.get(3))) {
            SimpleCheckboxList(items)
        }
    }
}