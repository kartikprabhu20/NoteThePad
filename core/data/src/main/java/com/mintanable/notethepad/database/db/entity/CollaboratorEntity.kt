package com.mintanable.notethepad.database.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_collaborators_local",
    indices = [
        Index(value = ["noteId"]),
        Index(value = ["collaboratorUserId"]),
        Index(value = ["ownerUserId"]),
        Index(value = ["noteId", "collaboratorUserId"], unique = true)
    ]
)
data class CollaboratorEntity(
    @PrimaryKey
    val id: String,
    val noteId: String,
    val ownerUserId: String,
    val collaboratorUserId: String,
    val collaboratorEmail: String,
    val collaboratorDisplayName: String? = null,
    val collaboratorPhotoUrl: String? = null
)
