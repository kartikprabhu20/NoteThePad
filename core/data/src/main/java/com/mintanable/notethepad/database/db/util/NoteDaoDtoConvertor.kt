package com.mintanable.notethepad.database.db.util

import com.mintanable.notethepad.core.network.sync.NoteDto
import com.mintanable.notethepad.core.network.sync.NoteTagCrossRefDto
import com.mintanable.notethepad.core.network.sync.TagDto
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.database.db.entity.NoteTagCrossRef
import com.mintanable.notethepad.database.db.entity.TagEntity

// Mapper extensions
fun NoteEntity.toDto() = NoteDto(
    id, title, content, timestamp, color, imageUris, audioUris,
    reminderTime, audioTranscriptions, backgroundImage, lastUpdateTime, userId, isDeleted
)

fun TagEntity.toDto() = TagDto(tagName, tagId, lastUpdateTime, userId, isDeleted)
fun NoteTagCrossRef.toDto() = NoteTagCrossRefDto(noteId, tagId, userId, isDeleted, lastUpdateTime)
