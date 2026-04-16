package com.mintanable.notethepad.feature_note.presentation

import androidx.compose.runtime.Stable
import com.mintanable.notethepad.core.richtext.model.RichTextState
import com.mintanable.notethepad.core.model.note.CheckboxItem
import com.mintanable.notethepad.database.db.entity.TagEntity

@Stable
data class EditNoteSnapshot(
    val titleState: NoteTextFieldState,
    val contentState: NoteTextFieldState ,
    val contentRichTextState: RichTextState,
    val noteColor: Int,
    val backgroundImage: Int,
    val reminderTime: Long,
    val checkListItems: List<CheckboxItem>,
    val isCheckboxListAvailable: Boolean,
    val tagEntities: List<TagEntity>
)
