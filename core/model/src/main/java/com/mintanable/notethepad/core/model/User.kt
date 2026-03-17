package com.mintanable.notethepad.core.model

data class User(
    val id: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isGoogleSignedIn: Boolean
)