package com.mintanable.notethepad.feature_settings.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.R
import com.mintanable.notethepad.core.model.Tag
import com.mintanable.notethepad.feature_note.presentation.modify.components.TagUI
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import com.mintanable.notethepad.ui.theme.ThemePreviews

@Composable
fun EditTextDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    tags: List<Tag> = emptyList()
) {
    var editTextFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val filteredTags by remember(editTextFieldValue.text, tags) {
        derivedStateOf {
            val list = if (editTextFieldValue.text.isBlank()) {
                tags
            } else {
                tags.filter { it.tagName.contains(editTextFieldValue.text, ignoreCase = true) }
            }
            list.take(3)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_title_new_tag), style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = editTextFieldValue,
                    onValueChange = { editTextFieldValue = it },
                    label = { Text(stringResource(R.string.label_tag_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.placeholder_tag_example)) }
                )

                if(tags.isNotEmpty()) {


                    FlowRow(modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {

                        Text(stringResource(R.string.label_existing), modifier = Modifier.padding(4.dp))

                        filteredTags.forEach{ tag ->
                            TagUI(
                                description = tag.tagName,
                                enableDeletion = false,
                                onClick = {
                                    editTextFieldValue = TextFieldValue(
                                        text = tag.tagName,
                                        selection = TextRange(tag.tagName.length)
                                    )
                                },
                                onDelete = {}
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (editTextFieldValue.text.isNotBlank()) {
                        onConfirm(editTextFieldValue.text.trim())
                    }
                },
                enabled = editTextFieldValue.text.isNotBlank() // Disable if empty
            ) {
                Text(stringResource(R.string.btn_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@ThemePreviews
@Composable
fun PreviewEditTextDialog(modifier: Modifier = Modifier) {
    NoteThePadTheme {
        EditTextDialog(
            onConfirm = {},
            onDismiss = {},
            tags = listOf(Tag("Grocery"), Tag("Shopping"), Tag("Travel"), Tag("home"), Tag("Expenses"))
        )
    }
}