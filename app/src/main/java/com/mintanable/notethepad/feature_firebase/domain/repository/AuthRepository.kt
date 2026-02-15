package com.mintanable.notethepad.feature_firebase.domain.repository

import com.mintanable.notethepad.feature_firebase.domain.model.User

interface AuthRepository {
    suspend fun loginWithEmail(email: String, pass: String): Result<User>
    suspend fun signInWithGoogle(token: String): Result<User>
    suspend fun signInWithFacebook(token: String): Result<User>
    fun getSignedInUser(): User?
    suspend fun signOut()
}