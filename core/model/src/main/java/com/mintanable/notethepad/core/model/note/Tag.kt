package com.mintanable.notethepad.core.model.note

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tag_table",
    indices = [Index(value = ["tagName"], unique = true)]
)
data class Tag(
    val tagName: String,
    @PrimaryKey(autoGenerate = true) val tagId: Long = 0
)
