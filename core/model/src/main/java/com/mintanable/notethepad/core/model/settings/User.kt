package com.mintanable.notethepad.core.model.settings

data class User(
    val id: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isGoogleSignedIn: Boolean
)