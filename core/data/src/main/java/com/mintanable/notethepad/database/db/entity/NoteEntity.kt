package com.mintanable.notethepad.database.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
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
    @PrimaryKey
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerialName("title")
    val title: String,

    @SerialName("content")
    val content: String,

    @SerialName("timestamp")
    val timestamp: Long,

    @SerialName("color")
    val color: Int,

    @SerialName("image_uris")
    val imageUris: List<String> = emptyList(),

    @SerialName("audio_uris")
    val audioUris: List<String> = emptyList(),

    @SerialName("reminder_time")
    val reminderTime: Long = -1,

    @SerialName("audio_transcriptions")
    val audioTranscriptions: String = "",

    @SerialName("background_image")
    val backgroundImage: Int = -1,

    @SerialName("last_update_time")
    val lastUpdateTime: Long = System.currentTimeMillis(),

    @SerialName("user_id")
    val userId: String? = null,

    @SerialName("is_deleted")
    val isDeleted: Boolean = false,

    @SerialName("is_synced")
    val isSynced: Boolean = false
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