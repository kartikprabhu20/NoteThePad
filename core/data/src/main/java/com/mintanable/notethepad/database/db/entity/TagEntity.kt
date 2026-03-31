package com.mintanable.notethepad.database.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tag_table",
    indices = [
        Index(value = ["tagName"], unique = true),
        Index(value = ["userId"]),
        Index(value = ["isDeleted"])
    ]
)
data class TagEntity(
    val tagName: String,
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val userId: String? = null,
    val isDeleted: Boolean = false,
    @PrimaryKey val tagId: String = UUID.randomUUID().toString()
)
