package com.mintanable.notethepad.feature_ai.tools

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.mintanable.notethepad.core.common.ReminderScheduler
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.database.db.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteTools @Inject constructor(
    private val noteRepository: NoteRepository,
    private val reminderScheduler: ReminderScheduler,
) : ToolSet {

    @Tool(description = "Creates a new note. Returns the new note's id.")
    fun createNote(
        @ToolParam(description = "Short title") title: String,
        @ToolParam(description = "Main text body") content: String,
        @ToolParam(description = "ARGB color int; 0 for default") color: Int,
        @ToolParam(description = "Reminder time in UTC ms; 0 for none") reminderMs: Double,
    ): String = runBlocking(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val reminder = reminderMs.toLong().takeIf { it > 0 } ?: -1L
        val entity = NoteEntity(
            title = title,
            content = content,
            timestamp = now,
            color = color,
            reminderTime = reminder,
            lastUpdateTime = now,
        )
        val noteId = noteRepository.insertNote(entity, emptyList())
        if (reminder > 0) {
            reminderScheduler.schedule(
                id = noteId.hashCode().toLong(),
                title = title,
                content = content.take(200),
                reminderTime = reminder,
            )
        }
        noteId
    }

    @Tool(description = "Updates an existing note's title, content, color, or reminder. Pass empty strings or 0 to keep fields unchanged. Returns 'ok' or 'note_not_found'.")
    fun updateNote(
        @ToolParam(description = "Note id") noteId: String,
        @ToolParam(description = "New title; empty to keep existing") title: String,
        @ToolParam(description = "New content; empty to keep existing") content: String,
        @ToolParam(description = "ARGB color int; 0 to keep existing") color: Int,
        @ToolParam(description = "Reminder time in UTC ms; 0 to keep existing, -1 to clear") reminderMs: Double,
    ): String = runBlocking(Dispatchers.IO) {
        val existing = noteRepository.getNoteById(noteId) ?: return@runBlocking "note_not_found"
        val current = existing.noteEntity
        val reminderLong = reminderMs.toLong()
        val newReminder = when {
            reminderLong > 0 -> reminderLong
            reminderLong < 0 -> -1L
            else -> current.reminderTime
        }
        val updated = current.copy(
            title = title.ifBlank { current.title },
            content = content.ifBlank { current.content },
            color = if (color != 0) color else current.color,
            reminderTime = newReminder,
            lastUpdateTime = System.currentTimeMillis(),
            isSynced = false,
        )
        noteRepository.insertNote(updated, existing.tagEntities)
        if (newReminder != current.reminderTime) {
            val alarmId = noteId.hashCode().toLong()
            if (newReminder > 0) {
                reminderScheduler.schedule(
                    id = alarmId,
                    title = updated.title,
                    content = updated.content.take(200),
                    reminderTime = newReminder,
                )
            } else {
                reminderScheduler.cancel(alarmId)
            }
        }
        "ok"
    }

    @Tool(description = "Soft-deletes a note by id. Returns 'ok'.")
    fun deleteNote(
        @ToolParam(description = "Note id") noteId: String,
    ): String = runBlocking(Dispatchers.IO) {
        noteRepository.deleteNoteWithId(noteId)
        reminderScheduler.cancel(noteId.hashCode().toLong())
        "ok"
    }

    @Tool(description = "Fetches a note by id and returns JSON {id,title,content,timestamp,reminderMs,color,summary,tags}.")
    fun getNote(
        @ToolParam(description = "Note id") noteId: String,
    ): String = runBlocking(Dispatchers.IO) {
        val note = noteRepository.getNoteById(noteId)
            ?: return@runBlocking """{"error":"not_found"}"""
        val n = note.noteEntity
        JSONObject().apply {
            put("id", n.id)
            put("title", n.title)
            put("content", n.content)
            put("timestamp", n.timestamp)
            put("reminderMs", n.reminderTime)
            put("color", n.color)
            put("summary", n.summary)
            put("tags", JSONArray(note.tagEntities.map { it.tagName }))
        }.toString()
    }

    @Tool(description = "Lists the N most recent notes as JSON array of {id,title,timestamp,tags}.")
    fun listRecentNotes(
        @ToolParam(description = "Maximum number of notes (1-50)") limit: Int,
    ): String = runBlocking(Dispatchers.IO) {
        val capped = limit.coerceIn(1, 50)
        val notes = noteRepository.getTopNotes(capped).first()
        val arr = JSONArray()
        notes.forEach { nwt ->
            val n = nwt.noteEntity
            arr.put(JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("timestamp", n.timestamp)
                put("tags", JSONArray(nwt.tagEntities.map { it.tagName }))
            })
        }
        arr.toString()
    }
}
