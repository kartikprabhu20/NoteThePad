package com.mintanable.notethepad.database.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

@Entity(
    indices = [
        Index(value = ["title"]),
        Index(value = ["timestamp"]),
        Index(value = ["color"]),
        Index(value = ["reminderTime"]),
        Index(value = ["userId"]),
        Index(value = ["isDeleted"])
    ]
)
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val timestamp: Long,
    val color: Int,
    val imageUris: List<String> = emptyList(),
    val audioUris: List<String> = emptyList(),
    val reminderTime: Long = -1,
    val audioTranscriptions: String = "",
    val backgroundImage: Int = -1,
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val userId: String? = null,
    val isDeleted: Boolean = false
)

class InvalidNoteException(message: String): Exception(message)

data class NoteWithTags(
    @Embedded val noteEntity: NoteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "tagId",
        associateBy = Junction(
            value = NoteTagCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "tagId"
        )
    )
    val tagEntities: List<TagEntity> = emptyList()
)