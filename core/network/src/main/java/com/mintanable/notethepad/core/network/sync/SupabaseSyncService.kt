package com.mintanable.notethepad.core.network.sync

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
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

    suspend fun syncNote(noteDto: NoteDto) {
        if (noteDto.userId != null) {
            supabaseClient.postgrest["note_entity"].upsert(noteDto)
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

    fun getSupabaseClient() = supabaseClient
}
