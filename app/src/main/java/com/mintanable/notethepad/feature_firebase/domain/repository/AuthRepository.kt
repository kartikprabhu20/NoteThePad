package com.mintanable.notethepad.feature_firebase.domain.repository

import com.mintanable.notethepad.feature_firebase.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun loginWithEmail(email: String, pass: String): Result<User>
    suspend fun signInWithGoogle(token: String): Result<User>
    suspend fun signInWithFacebook(token: String): Result<User>
    fun getSignedInFirebaseUser(): Flow<User?>
    suspend fun signOut()
    fun isUserSignedInWithGoogle(): Boolean
}