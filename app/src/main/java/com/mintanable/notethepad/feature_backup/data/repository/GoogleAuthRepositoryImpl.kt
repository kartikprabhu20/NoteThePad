package com.mintanable.notethepad.feature_backup.data.repository

import android.accounts.Account
import android.content.Context
import android.content.Intent
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
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleAuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.mintanable.notethepad.BuildConfig
import kotlinx.coroutines.flow.first
import android.util.Log
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleAuthRepositoryImpl @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
): GoogleAuthRepository {
    companion object {
        val REFRESH_TOKEN_KEY = stringPreferencesKey("google_refresh_token")
        val clientId = BuildConfig.DRIVE_CLIENT_ID
        val clientSecret = BuildConfig.DRIVE_CLIENT_SECRET
    }

    override suspend fun authorizeDriveAccess(accountEmail: String): Result<AuthorizationResult> {
        return try {
            val requestedScopes = listOf(Scope(DriveScopes.DRIVE_APPDATA))
            val googleAccount = Account(accountEmail, "com.google")
            val authorizationRequestBuilder = AuthorizationRequest.builder()
                .setRequestedScopes(requestedScopes)
                .requestOfflineAccess("$clientId")
                .setAccount(googleAccount)

            if(!hasDriveAccess()){
                authorizationRequestBuilder.setPrompt(AuthorizationRequest.Prompt.CONSENT)
            }

            val result = Identity.getAuthorizationClient(context)
                .authorize(authorizationRequestBuilder.build())
                .await()

            Result.success(result)
        } catch (e: Exception) {
            Log.d("kptest", "Exception while authorizeDriveAccess $e ")
            Result.failure(e)
        }
    }

    override suspend fun exchangeCodeForTokens(authCode: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val tokenResponse = GoogleAuthorizationCodeTokenRequest(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    "https://oauth2.googleapis.com/token",
                    clientId,
                    clientSecret,
                    authCode,
                    ""
                ).execute()

                val rawToken = tokenResponse.refreshToken
                if (rawToken != null) {
                    val encryptedToken = cryptoManager.encrypt(rawToken)
                    dataStore.edit { preferences ->
                        preferences[REFRESH_TOKEN_KEY] = encryptedToken
                    }
                    Result.success(Unit)
                } else {
                    Log.e("kptest", "Refresh Token was NULL. Force a re-auth or check console.")
                    Result.failure(Exception("Refresh Token missing. Please revoke app access in Google settings and try again."))
                }
            } catch (e: Exception) {
                Log.e("kptest", "Exception while handling exchangeCodeForTokens $e")
                Result.failure(e)
            }
        }
    }

    override suspend fun hasDriveAccess(): Boolean {
        val encryptedToken = dataStore.data.first()[REFRESH_TOKEN_KEY]
        return !encryptedToken.isNullOrEmpty()
    }

    override suspend fun getDecryptedRefreshToken(): String? {
        val encryptedToken = dataStore.data.first()[REFRESH_TOKEN_KEY] ?: return null
        return cryptoManager.decrypt(encryptedToken)
    }

    override suspend fun refreshAccessToken(refreshToken: String): String {
        return withContext(Dispatchers.IO) {
            val response = GoogleRefreshTokenRequest(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                refreshToken,
                clientId,
                clientSecret
            ).execute()

            response.accessToken // This lasts for 1 hour
        }
    }

    override suspend fun getAuthCodeFromIntent(data: Intent?): String? {
        if (data == null) return null

        return try {
            val result = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(data)

            result.serverAuthCode
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearDriveCredentials() {
        dataStore.edit { prefs ->
            prefs.remove(REFRESH_TOKEN_KEY)
        }
    }
}