package com.mintanable.notethepad.core.model.settings

data class User(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isGoogleSignedIn: Boolean
)