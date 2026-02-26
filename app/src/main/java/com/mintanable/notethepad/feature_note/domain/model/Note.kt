package com.mintanable.notethepad.feature_note.domain.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.lang.Exception

@Immutable
@Entity(
    indices = [
        Index(value = ["title"]),
        Index(value = ["timestamp"]),
        Index(value = ["color"])
    ]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val title: String,
    val content: String,
    val timestamp: Long,
    val color: Int,
    val imageUris: List<String> = emptyList(),
    val audioUris: List<String> = emptyList()
)

class InvalidNoteException(message: String):Exception(message)