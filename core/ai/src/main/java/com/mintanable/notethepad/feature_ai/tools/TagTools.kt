package com.mintanable.notethepad.feature_ai.tools

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagTools @Inject constructor(
    private val noteRepository: NoteRepository,
    private val assistantRepositoryLazy: Lazy<NoteAssistantRepository>,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ToolSet {

    @Tool(description = "Lists all existing tags as a JSON array of {id,name}.")
    fun listAllTags(): String = runBlocking(Dispatchers.IO) {
        val tags = noteRepository.getAllTags().first()
        val arr = JSONArray()
        tags.filter { !it.isDeleted }.forEach { tag ->
            arr.put(JSONObject().apply {
                put("id", tag.tagId)
                put("name", tag.tagName)
            })
        }
        arr.toString()
    }

    @Tool(description = "Creates a tag if it doesn't exist. Returns the tag id.")
    fun createTag(
        @ToolParam(description = "Tag name") name: String,
    ): String = runBlocking(Dispatchers.IO) {
        val existing = noteRepository.getTagByName(name)
        if (existing != null && !existing.isDeleted) return@runBlocking existing.tagId
        val tag = existing?.copy(isDeleted = false, lastUpdateTime = System.currentTimeMillis())
            ?: TagEntity(tagName = name)
        if (existing == null) {
            noteRepository.insertTag(tag)
        } else {
            noteRepository.updateTag(tag)
        }
        tag.tagId
    }

    @Tool(description = "Attaches a tag (by name) to a note. Creates the tag if missing. Returns 'ok' or 'note_not_found'.")
    fun addTagToNote(
        @ToolParam(description = "Note id") noteId: String,
        @ToolParam(description = "Tag name") tagName: String,
    ): String = runBlocking(Dispatchers.IO) {
        val note = noteRepository.getNoteById(noteId) ?: return@runBlocking "note_not_found"
        if (note.tagEntities.any { it.tagName.equals(tagName, ignoreCase = true) }) {
            return@runBlocking "ok"
        }
        val tag = noteRepository.getTagByName(tagName)?.copy(isDeleted = false)
            ?: TagEntity(tagName = tagName)
        val merged = note.tagEntities + tag
        noteRepository.insertNote(note.noteEntity, merged)
        "ok"
    }

    @Tool(description = "Removes a tag (by name) from a note. Returns 'ok' or 'note_not_found'.")
    fun removeTagFromNote(
        @ToolParam(description = "Note id") noteId: String,
        @ToolParam(description = "Tag name") tagName: String,
    ): String = runBlocking(Dispatchers.IO) {
        val note = noteRepository.getNoteById(noteId) ?: return@runBlocking "note_not_found"
        val remaining = note.tagEntities.filterNot { it.tagName.equals(tagName, ignoreCase = true) }
        if (remaining.size == note.tagEntities.size) return@runBlocking "ok"
        noteRepository.insertNote(note.noteEntity, remaining)
        "ok"
    }

    @Tool(description = "Asks the current AI model to suggest tags for a note. Returns JSON array of tag names.")
    fun suggestTags(
        @ToolParam(description = "Note id") noteId: String,
    ): String = runBlocking(Dispatchers.IO) {
        val note = noteRepository.getNoteById(noteId) ?: return@runBlocking "[]"
        val modelName = userPreferencesRepository.settingsFlow.first().aiModelName
        if (modelName == "None") return@runBlocking "[]"
        val existingTags = noteRepository.getAllTags().first().map { it.tagName }
        val suggested = assistantRepositoryLazy.get().suggestTags(
            title = note.noteEntity.title,
            content = note.noteEntity.content,
            existingTags = existingTags,
            modelName = modelName,
        )
        JSONArray(suggested).toString()
    }
}
