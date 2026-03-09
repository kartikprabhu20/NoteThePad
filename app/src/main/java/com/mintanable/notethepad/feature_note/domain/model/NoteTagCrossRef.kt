package com.mintanable.notethepad.feature_note.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index


@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagName"],
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE // If note is deleted, the link is deleted
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["tagName"],
            childColumns = ["tagName"],
            onDelete = ForeignKey.CASCADE // If tag is deleted, the link is deleted
        )
    ],
    indices = [Index("tagName")]
)
data class NoteTagCrossRef(
    val noteId: Long,
    val tagName: String
)