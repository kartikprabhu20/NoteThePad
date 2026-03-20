package com.mintanable.notethepad.database.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tag_table",
    indices = [Index(value = ["tagName"], unique = true)]
)
data class TagEntity(
    val tagName: String,
    @PrimaryKey(autoGenerate = true) val tagId: Long = 0
)
