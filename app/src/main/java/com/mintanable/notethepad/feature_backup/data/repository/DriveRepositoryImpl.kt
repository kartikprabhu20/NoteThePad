package com.mintanable.notethepad.feature_backup.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.DriveScopes
import com.mintanable.notethepad.core.security.CryptoManager
import com.mintanable.notethepad.feature_backup.domain.repository.DriveRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.mintanable.notethepad.BuildConfig

class DriveRepositoryImpl @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val dataStore: DataStore<Preferences>,
    private val context: Context
): DriveRepository {
    companion object {
        val REFRESH_TOKEN_KEY = stringPreferencesKey("google_refresh_token")
        val clientId = BuildConfig.DRIVE_CLIENT_ID
        val clientSecret = BuildConfig.DRIVE_CLIENT_SECRET
    }

    override suspend fun authorizeDriveAccess(): Result<AuthorizationResult> {
        return try {
            val requestedScopes = listOf(Scope(DriveScopes.DRIVE_APPDATA))
            val authorizationRequest = AuthorizationRequest.builder()
                .setRequestedScopes(requestedScopes)
                .requestOfflineAccess("$clientId.apps.googleusercontent.com")
                .build()

            val result = Identity.getAuthorizationClient(context)
                .authorize(authorizationRequest)
                .await()

            Result.success(result)

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exchangeCodeForTokens(authCode: String): Result<Unit> {
        return try {
            val tokenResponse = GoogleAuthorizationCodeTokenRequest(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                "https://oauth2.googleapis.com/token",
                clientId,
                clientSecret,
                authCode,
                ""
            ).execute()

            tokenResponse.refreshToken?.let { rawToken ->
                val encryptedToken = cryptoManager.encrypt(rawToken)
                dataStore.edit { preferences ->
                    preferences[REFRESH_TOKEN_KEY] = encryptedToken
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}