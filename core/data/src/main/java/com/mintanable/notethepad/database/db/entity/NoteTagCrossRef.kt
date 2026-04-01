package com.mintanable.notethepad.database.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
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
    @SerialName("note_id") // Supabase: note_id
    val noteId: String,    // Room: noteId

    @SerialName("tag_id")
    val tagId: String,

    @SerialName("user_id")
    val userId: String? = null,

    @SerialName("is_deleted")
    val isDeleted: Boolean = false,

    @SerialName("last_update_time")
    val lastUpdateTime: Long = System.currentTimeMillis()
)