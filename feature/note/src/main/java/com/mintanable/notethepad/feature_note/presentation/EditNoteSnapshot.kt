package com.mintanable.notethepad.feature_note.presentation

import androidx.compose.runtime.Stable
import com.mintanable.notethepad.core.richtext.model.RichTextDocument
import com.mintanable.notethepad.core.model.note.CheckboxItem
import com.mintanable.notethepad.database.db.entity.TagEntity

@Stable
data class EditNoteSnapshot(
    val titleDocument: RichTextDocument,
    val contentDocument: RichTextDocument,
    val noteColor: Int,
    val backgroundImage: Int,
    val reminderTime: Long,
    val checkListItems: List<CheckboxItem>,
    val isCheckboxListAvailable: Boolean,
    val tagEntities: List<TagEntity>
)
