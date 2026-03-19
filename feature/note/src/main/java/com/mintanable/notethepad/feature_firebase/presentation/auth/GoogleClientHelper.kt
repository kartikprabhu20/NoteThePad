package com.mintanable.notethepad.feature_firebase.presentation.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleClientHelper(private val context: Context,private val clientId: String) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun getGoogleCredential(): String? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            // 4. Extract the ID Token
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            googleIdTokenCredential.idToken
        } catch (e: GetCredentialException) {
            Log.e("Auth", "Credential Manager Error: ${e.message}")
            null
        }
    }

    suspend fun clearCredentials() {
        try {
            val clearRequest = ClearCredentialStateRequest()
            credentialManager.clearCredentialState(clearRequest)
        } catch (e: ClearCredentialException) {
            Log.e("Auth", "Couldn't clear user credentials: ${e.localizedMessage}")
        }
    }
}