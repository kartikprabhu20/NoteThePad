package com.mintanable.notethepad.feature_ai.tools

import android.content.Context
import androidx.core.net.toUri
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.mintanable.notethepad.core.common.utils.readAndProcessImage
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaTools @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository,
    private val assistantRepositoryLazy: Lazy<NoteAssistantRepository>,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ToolSet {

    @Tool(description = "Lists attachments on a note as JSON {images:[uri,...], audios:[uri,...]}.")
    fun listAttachments(
        @ToolParam(description = "Note id") noteId: String,
    ): String = runBlocking(Dispatchers.IO) {
        val note = noteRepository.getNoteById(noteId)?.noteEntity
            ?: return@runBlocking """{"error":"not_found"}"""
        JSONObject().apply {
            put("images", JSONArray(note.imageUris))
            put("audios", JSONArray(note.audioUris))
        }.toString()
    }

    @Tool(description = "Transcribes an audio attachment using the current AI model. Returns the transcript text.")
    fun transcribeAudioAttachment(
        @ToolParam(description = "Audio uri string (as stored on the note)") audioUri: String,
    ): String = runBlocking(Dispatchers.IO) {
        val modelName = userPreferencesRepository.settingsFlow.first().aiModelName
        if (modelName == "None") return@runBlocking "ai_disabled"
        val transcript = StringBuilder()
        runCatching {
            assistantRepositoryLazy.get().transcribeAudioFile(audioUri, modelName) { chunk ->
                transcript.append(chunk)
            }
        }.onFailure { return@runBlocking "transcription_failed" }
        transcript.toString().ifBlank { "no_speech_detected" }
    }

    @Tool(description = "Describes an image attachment using the current AI model. Returns a short caption.")
    fun describeImageAttachment(
        @ToolParam(description = "Image uri string (as stored on the note)") imageUri: String,
    ): String = runBlocking(Dispatchers.IO) {
        val modelName = userPreferencesRepository.settingsFlow.first().aiModelName
        if (modelName == "None") return@runBlocking "ai_disabled"
        val bytes = runCatching { readAndProcessImage(context, imageUri.toUri(), maxDimension = 1024) }
            .getOrNull() ?: return@runBlocking "image_read_failed"
        val description = runCatching { assistantRepositoryLazy.get().describeImage(bytes, modelName) }
            .getOrNull()
        description?.takeIf { it.isNotBlank() } ?: "no_description"
    }
}
