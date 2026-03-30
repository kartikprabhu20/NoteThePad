package com.mintanable.notethepad.database.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    indices = [
        Index(value = ["title"]),
        Index(value = ["timestamp"]),
        Index(value = ["color"]),
        Index(value = ["reminderTime"])
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val content: String,
    val timestamp: Long,
    val color: Int,
    val imageUris: List<String> = emptyList(),
    val audioUris: List<String> = emptyList(),
    val reminderTime: Long = -1,
    val audioTranscriptions: String = "",
    val backgroundImage: Int = -1
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