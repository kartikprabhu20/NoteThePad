package com.mintanable.notethepad.feature_note.presentation.modify.components.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_note.domain.model.NoteColors
import com.mintanable.notethepad.feature_note.domain.model.Tag
import com.mintanable.notethepad.feature_note.presentation.modify.components.TagUI
import com.mintanable.notethepad.feature_note.presentation.notes.TagType
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import com.mintanable.notethepad.ui.theme.ThemePreviews

fun LazyListScope.tagsSection(
    tags: List<Tag>,
    onDelete: (String) -> Unit,
){
    if(tags.isNotEmpty()) {
        item {
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            tags.forEach { tag ->
                    Box(
                        modifier = Modifier.padding(4.dp)
                    ) {
                        TagUI(
                            imageVector = TagType.LABEL_TAG.imageVector,
                            description = tag.tagName,
                            onDelete = {onDelete(tag.tagName)},
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
fun PreviewTagSection() {
    NoteThePadTheme {
        val tags = listOf(Tag("abcdefghijklmnop"),Tag("testing"),Tag("1234"), Tag("b"),Tag("xyztesting2"),Tag("9876"))
       LazyColumn(
           modifier = Modifier.fillMaxWidth().background(NoteColors.colors[0])
       ) {
           tagsSection(tags = tags, onDelete = {})
       }
    }
}