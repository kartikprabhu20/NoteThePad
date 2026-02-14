package com.mintanable.notethepad.feature_note.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.lang.Exception

@Entity
data class Note(val title: String,
                val content: String,
                val timestamp: Long,
                val color: Int,
                @PrimaryKey val id: Int? = null
)

class InvalidNoteException(message: String):Exception(message)