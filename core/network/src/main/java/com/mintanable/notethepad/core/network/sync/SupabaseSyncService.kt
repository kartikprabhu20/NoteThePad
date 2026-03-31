package com.mintanable.notethepad.core.network.sync

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class NoteDto(
    val id: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val color: Int,
    val imageUris: List<String>,
    val audioUris: List<String>,
    val reminderTime: Long,
    val audioTranscriptions: String,
    val backgroundImage: Int,
    val lastUpdateTime: Long,
    val userId: String?,
    val isDeleted: Boolean
)

@Serializable
data class TagDto(
    val tagName: String,
    val tagId: String,
    val lastUpdateTime: Long,
    val userId: String?,
    val isDeleted: Boolean
)

@Serializable
data class NoteTagCrossRefDto(
    val noteId: String,
    val tagId: String,
    val userId: String?,
    val isDeleted: Boolean,
    val lastUpdateTime: Long
)

@Singleton
class SupabaseSyncService @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    suspend fun syncNote(noteDto: NoteDto) {
        if (noteDto.userId != null) {
            supabaseClient.postgrest["NoteEntity"].upsert(noteDto)
        }
    }

    suspend fun syncTag(tagDto: TagDto) {
        if (tagDto.userId != null) {
            supabaseClient.postgrest["tag_table"].upsert(tagDto)
        }
    }

    suspend fun syncCrossRef(crossRefDto: NoteTagCrossRefDto) {
        if (crossRefDto.userId != null) {
            supabaseClient.postgrest["note_tag_cross_ref"].upsert(crossRefDto)
        }
    }
}
