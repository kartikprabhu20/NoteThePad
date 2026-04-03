package com.mintanable.notethepad.core.network.sync

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollaborationService @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    suspend fun ensureAuthenticated(fireBaseToken: String?) {
        try {
            if (fireBaseToken != null) {
                supabaseClient.auth.importSession(
                    UserSession(
                        accessToken = fireBaseToken,
                        refreshToken = "",
                        expiresIn = 3600,
                        tokenType = "bearer",
                        user = null
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import Firebase token: ${e.message}")
        }
    }

    suspend fun upsertUserProfile(
        userId: String,
        email: String,
        displayName: String?,
        photoUrl: String?
    ): Boolean {
        return try {
            val dto = UserProfileDto(
                id = userId,
                email = email,
                displayName = displayName,
                photoUrl = photoUrl
            )
            supabaseClient.from("user_profiles").upsert(dto)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upsert user profile: ${e.message}")
            false
        }
    }

    suspend fun findUserByEmail(email: String): FindUserResult? {
        return try {
            val result = supabaseClient.postgrest.rpc(
                "find_user_by_email",
                buildJsonObject { put("lookup_email", email) }
            )
            val users = result.decodeList<FindUserResult>()
            users.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find user by email: ${e.message}")
            null
        }
    }

    suspend fun addCollaborator(dto: NoteCollaboratorDto): Boolean {
        return try {
            supabaseClient.from("note_collaborators").insert(dto) {
                select()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add collaborator: ${e.message}")
            false
        }
    }

    suspend fun removeCollaborator(noteId: String, collaboratorUserId: String): Boolean {
        return try {
            supabaseClient.from("note_collaborators").delete {
                filter {
                    eq("note_id", noteId)
                    eq("collaborator_user_id", collaboratorUserId)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove collaborator: ${e.message}")
            false
        }
    }

    suspend fun getCollaborators(noteId: String): List<NoteCollaboratorDto> {
        return try {
            supabaseClient.from("note_collaborators")
                .select {
                    filter { eq("note_id", noteId) }
                }
                .decodeList<NoteCollaboratorDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get collaborators: ${e.message}")
            emptyList()
        }
    }

    suspend fun getMyCollaborations(userId: String): List<NoteCollaboratorDto> {
        return try {
            supabaseClient.from("note_collaborators")
                .select {
                    filter { eq("collaborator_user_id", userId) }
                }
                .decodeList<NoteCollaboratorDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get my collaborations: ${e.message}")
            emptyList()
        }
    }

    suspend fun getNotesIShared(userId: String): List<NoteCollaboratorDto> {
        return try {
            supabaseClient.from("note_collaborators")
                .select {
                    filter { eq("owner_user_id", userId) }
                }
                .decodeList<NoteCollaboratorDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get notes I shared: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchSharedNotes(userId: String): List<NoteDto> {
        return try {
            val result = supabaseClient.postgrest.rpc(
                "get_shared_notes",
                buildJsonObject { put("uid", userId) }
            )
            result.decodeList<NoteDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch shared notes: ${e.message}")
            emptyList()
        }
    }

    companion object {
        private const val TAG = "CollaborationService"
    }
}
