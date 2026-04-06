package com.mintanable.notethepad.auth.repository

import android.util.Log
import androidx.credentials.exceptions.ClearCredentialException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.mintanable.notethepad.core.model.settings.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override suspend fun loginWithEmail(email: String, pass: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            Result.success(result.user!!.toDomainUser())
        } catch (e: Exception) { 
            Log.e("AuthRepository", "loginWithEmail error: ${e.localizedMessage}")
            Result.failure(e) 
        }
    }

    override suspend fun signInWithGoogle(token: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(token, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user
            if(user != null){
                Result.success(user.toDomainUser())
            }else{
                Result.failure(Exception("Firebase user is null"))
            }
        } catch (e: Exception) { 
            Log.e("AuthRepository", "signInWithGoogle error: ${e.localizedMessage}")
            Result.failure(e) 
        }
    }

    override suspend fun signInWithFacebook(token: String): Result<User> {
        return try {
            val credential = FacebookAuthProvider.getCredential(token)
            val result = firebaseAuth.signInWithCredential(credential).await()
            Result.success(result.user!!.toDomainUser())
        } catch (e: Exception) { 
            Log.e("AuthRepository", "signInWithFacebook error: ${e.localizedMessage}")
            Result.failure(e) 
        }
    }

    override fun getSignedInFirebaseUser(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            try {
                this.trySend(auth.currentUser?.toDomainUser())
            } catch (e: Exception) {
                Log.e("AuthRepository", "AuthStateListener error: ${e.localizedMessage}")
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    override suspend fun getFreshFirebaseToken(): String? {
        return try {
            firebaseAuth.currentUser?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            Log.e("AuthRepository", "getFreshFirebaseToken failed: ${e.localizedMessage}")
            null
        }
    }

    override suspend fun signOut() {
        try {
            firebaseAuth.signOut()
        } catch (e: Exception) {
            Log.e("AuthRepository", "signOut error: ${e.localizedMessage}")
        }
    }

    override fun isUserSignedInWithGoogle(): Boolean {
        val user = firebaseAuth.currentUser
        return user?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } ?: false
    }
}

private fun FirebaseUser.toDomainUser() = User(uid, email, displayName, photoUrl.toString(),
    providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID })
