package com.mintanable.notethepad.core.network.sync

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseSyncService @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    fun setSyncing(syncing: Boolean) {
        _isSyncing.value = syncing
    }

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
            Log.e("Sync", "Failed to refresh Firebase token: ${e.message}")
        }
    }

    suspend fun syncNote(noteDto: NoteDto): Boolean {
        Log.d("Sync", "syncNote")

        return try {
            val result: PostgrestResult = supabaseClient.from("note_entity").upsert(noteDto) {
                select() //to use decodeAs() later
            }

            val isSuccess = result.data != "[]" && result.data.isNotEmpty()

            if (isSuccess) {
                val confirmedNote = result.decodeSingle<NoteDto>()
                Log.d("Sync", "Server confirmed Note ID: ${confirmedNote.id}")
            }

            isSuccess
        } catch (e: Exception) {
            Log.e("Sync", "Postgrest Error: ${e.message}")
            false
        }
    }

    suspend fun updateSharedNote(noteDto: NoteDto): Boolean {
        return try {
            // Use RPC (POST) instead of .update() (PATCH) to avoid HTTP method issues
            val result = supabaseClient.postgrest.rpc(
                "update_shared_note",
                buildJsonObject {
                    put("p_note_id", noteDto.id)
                    put("p_title", noteDto.title)
                    put("p_content", noteDto.content)
                    put("p_timestamp", noteDto.timestamp)
                    put("p_color", noteDto.color)
                    putJsonArray("p_image_uris") { noteDto.imageUris.forEach { add(it) } }
                    putJsonArray("p_audio_uris") { noteDto.audioUris.forEach { add(it) } }
                    put("p_reminder_time", noteDto.reminderTime)
                    put("p_audio_transcriptions", noteDto.audioTranscriptions)
                    put("p_background_image", noteDto.backgroundImage)
                    put("p_last_update_time", noteDto.lastUpdateTime)
                    put("p_is_deleted", noteDto.isDeleted)
                }
            )
            val isSuccess = result.data != "[]" && result.data.isNotEmpty()
            if (isSuccess) {
                Log.d("Sync", "Shared note updated: ${noteDto.id}")
            }
            isSuccess
        } catch (e: Exception) {
            Log.e("Sync", "Shared note update error: ${e.message}")
            false
        }
    }

    suspend fun syncTag(tagDto: TagDto): Boolean {
        if (tagDto.userId == null) return false

        return try {
            val result = supabaseClient.from("tag_table").upsert(tagDto) {
                select()
            }

            val isSuccess = result.data != "[]" && result.data.isNotEmpty()
            if (isSuccess) {
                Log.d("SupabaseSync", "Tag synced: ${tagDto.tagId}")
            }
            isSuccess
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to sync tag ${tagDto.tagId}: ${e.message}")
            false
        }
    }

    suspend fun syncCrossRef(crossRefDto: NoteTagCrossRefDto): Boolean {
        if (crossRefDto.userId == null) return false

        return try {
            val result = supabaseClient.from("note_tag_cross_ref").upsert(crossRefDto) {
                select()
            }

            val isSuccess = result.data != "[]" && result.data.isNotEmpty()
            if (isSuccess) {
                Log.d("SupabaseSync", "CrossRef synced: Note ${crossRefDto.noteId} <-> Tag ${crossRefDto.tagId}")
            }
            isSuccess
        } catch (e: Exception) {
            // This often fails if the Note or Tag hasn't reached the server yet
            Log.e("SupabaseSync", "CrossRef sync failed (Check FK constraints): ${e.message}")
            false
        }
    }

    /**
     * Upsert a tag on a shared note via RPC so the collaborator can write
     * to tag_table with the owner's user_id (bypasses RLS).
     */
    suspend fun upsertSharedTag(tagDto: TagDto): Boolean {
        if (tagDto.userId == null) return false
        return try {
            val result = supabaseClient.postgrest.rpc(
                "upsert_shared_tag",
                buildJsonObject {
                    put("p_tag_id", tagDto.tagId)
                    put("p_tag_name", tagDto.tagName)
                    put("p_user_id", tagDto.userId)
                    put("p_last_update_time", tagDto.lastUpdateTime)
                    put("p_is_deleted", tagDto.isDeleted)
                }
            )
            val isSuccess = result.data != "[]" && result.data.isNotEmpty()
            if (isSuccess) Log.d("SupabaseSync", "Shared tag upserted: ${tagDto.tagId}")
            isSuccess
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Shared tag upsert failed: ${e.message}")
            false
        }
    }

    /**
     * Upsert a cross-ref on a shared note via RPC so the collaborator can write
     * to note_tag_cross_ref with the owner's user_id (bypasses RLS).
     */
    suspend fun upsertSharedCrossRef(crossRefDto: NoteTagCrossRefDto): Boolean {
        if (crossRefDto.userId == null) return false
        return try {
            val result = supabaseClient.postgrest.rpc(
                "upsert_shared_cross_ref",
                buildJsonObject {
                    put("p_note_id", crossRefDto.noteId)
                    put("p_tag_id", crossRefDto.tagId)
                    put("p_user_id", crossRefDto.userId)
                    put("p_is_deleted", crossRefDto.isDeleted)
                    put("p_last_update_time", crossRefDto.lastUpdateTime)
                }
            )
            val isSuccess = result.data != "[]" && result.data.isNotEmpty()
            if (isSuccess) Log.d("SupabaseSync", "Shared cross-ref upserted: ${crossRefDto.noteId} <-> ${crossRefDto.tagId}")
            isSuccess
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Shared cross-ref upsert failed: ${e.message}")
            false
        }
    }

    suspend fun fetchNotes(userId: String): List<NoteDto> {
        return supabaseClient.postgrest["note_entity"]
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<NoteDto>()
    }

    suspend fun fetchTags(userId: String): List<TagDto> {
        return supabaseClient.postgrest["tag_table"]
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<TagDto>()
    }

    suspend fun fetchCrossRefs(userId: String): List<NoteTagCrossRefDto> {
        return supabaseClient.postgrest["note_tag_cross_ref"]
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<NoteTagCrossRefDto>()
    }

    suspend fun fetchCrossRefsForNotes(noteIds: List<String>): List<NoteTagCrossRefDto> {
        if (noteIds.isEmpty()) return emptyList()
        return try {
            supabaseClient.postgrest["note_tag_cross_ref"]
                .select {
                    filter {
                        isIn("note_id", noteIds)
                    }
                }
                .decodeList<NoteTagCrossRefDto>()
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to fetch cross-refs for shared notes: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchTagsByIds(tagIds: List<String>): List<TagDto> {
        if (tagIds.isEmpty()) return emptyList()
        return try {
            supabaseClient.postgrest["tag_table"]
                .select {
                    filter {
                        isIn("tag_id", tagIds)
                    }
                }
                .decodeList<TagDto>()
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to fetch tags by IDs: ${e.message}")
            emptyList()
        }
    }

    fun getSupabaseClient() = supabaseClient
}
