package com.mintanable.notethepad.database.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(
    tableName = "tag_table",
    indices = [
        Index(value = ["tagName"], unique = true),
        Index(value = ["userId"]),
        Index(value = ["isDeleted"])
    ]
)
data class TagEntity(
    @SerialName("tag_name")
    val tagName: String,

    @SerialName("last_update_time")
    val lastUpdateTime: Long = System.currentTimeMillis(),

    @SerialName("user_id")
    val userId: String? = null,

    @SerialName("is_deleted")
    val isDeleted: Boolean = false,

    @SerialName("is_synced")
    val isSynced: Boolean = false,

    @PrimaryKey
    @SerialName("tag_id")
    val tagId: String = UUID.randomUUID().toString()
)
