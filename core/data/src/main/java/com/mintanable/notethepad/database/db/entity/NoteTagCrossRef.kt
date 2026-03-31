package com.mintanable.notethepad.database.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["tagId"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("tagId"),
        Index("noteId"),
        Index("userId"),
        Index("isDeleted")
    ]
)
data class NoteTagCrossRef(
    val noteId: String,
    val tagId: String,
    val userId: String? = null,
    val isDeleted: Boolean = false,
    val lastUpdateTime: Long = System.currentTimeMillis()
)