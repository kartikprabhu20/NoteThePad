package com.mintanable.notethepad.feature_note.domain.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "tag_table",
    indices = [Index(value = ["tagName"], unique = true)]
)data class Tag(
    val tagName: String,
    @PrimaryKey(autoGenerate = true) val tagId: Long = 0
)


data class TagWithNotes(
    @Embedded val tag: Tag,
    @Relation(
        parentColumn = "tagName",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteTagCrossRef::class,
            parentColumn = "tagName",
            entityColumn = "noteId"
        )
    )
    val notes: List<Note> = emptyList()
)