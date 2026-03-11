package com.mintanable.notethepad.feature_note.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index


@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE // If note is deleted, the link is deleted
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["tagId"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE // If tag is deleted, the link is deleted
        )
    ],
    indices = [Index("tagId"),Index("noteId")]
)
data class NoteTagCrossRef(
    val noteId: Long,
    val tagId: Long
)