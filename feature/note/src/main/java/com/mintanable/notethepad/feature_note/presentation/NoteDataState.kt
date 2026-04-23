package com.mintanable.notethepad.feature_note.presentation

import android.net.Uri
import com.mintanable.notethepad.core.model.note.CheckboxItem
import com.mintanable.notethepad.database.db.entity.Attachment
import com.mintanable.notethepad.database.db.entity.TagEntity

data class NoteDataState(
    val title: String,
    val content: String,
    val noteColor: Int,
    val backgroundImage: Int,
    val attachedImages: List<Uri>,
    val attachedPaints: List<Uri>,
    val attachedAudios: List<Attachment>,
    val reminderTime: Long,
    val checkListItems: List<CheckboxItem>,
    val isCheckboxListAvailable: Boolean,
    val tagEntities: List<TagEntity>,
    val summary: String
)
