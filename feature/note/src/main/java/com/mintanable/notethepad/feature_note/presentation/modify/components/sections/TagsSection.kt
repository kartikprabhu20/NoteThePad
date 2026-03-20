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
import com.mintanable.notethepad.feature_note.presentation.notes.components.TagUI
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.feature_note.domain.util.TagType
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews

fun LazyListScope.tagsSection(
    tagEntities: List<TagEntity>,
    onDelete: (String) -> Unit,
){
    if(tagEntities.isNotEmpty()) {
        item {
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            tagEntities.forEach { tag ->
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
        val tagEntities = listOf(TagEntity("abcdefghijklmnop"),TagEntity("testing"),TagEntity("1234"), TagEntity("b"),TagEntity("xyztesting2"),TagEntity("9876"))
       LazyColumn(
           modifier = Modifier.fillMaxWidth().background(NoteColors.colors[0])
       ) {
           tagsSection(tagEntities = tagEntities, onDelete = {})
       }
    }
}