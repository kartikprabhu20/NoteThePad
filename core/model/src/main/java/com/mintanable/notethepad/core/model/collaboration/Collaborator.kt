package com.mintanable.notethepad.core.model.collaboration

data class Collaborator(
    val id: String,
    val noteId: String,
    val userId: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val isOwner: Boolean = false
)
