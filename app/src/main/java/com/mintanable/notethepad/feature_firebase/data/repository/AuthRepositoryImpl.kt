package com.mintanable.notethepad.feature_firebase.data.repository

import android.util.Log
import androidx.credentials.exceptions.ClearCredentialException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.mintanable.notethepad.feature_firebase.domain.model.User
import com.mintanable.notethepad.feature_firebase.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override suspend fun loginWithEmail(email: String, pass: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            Result.success(result.user!!.toDomainUser())
        } catch (e: Exception) { Result.failure(e) }
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
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun signInWithFacebook(token: String): Result<User> {
        return try {
            val credential = FacebookAuthProvider.getCredential(token)
            val result = firebaseAuth.signInWithCredential(credential).await()
            Result.success(result.user!!.toDomainUser())
        } catch (e: Exception) { Result.failure(e) }
    }

    override fun getSignedInUser(): User? {
        return firebaseAuth.currentUser?.toDomainUser()
    }

    override suspend fun signOut() {
        try {
            firebaseAuth.signOut()

        } catch (e: ClearCredentialException) {
            Log.e("Auth", "Couldn't clear user credentials: ${e.localizedMessage}")
        }
    }

}

fun FirebaseUser.toDomainUser() = User(uid, email, displayName, photoUrl.toString())
