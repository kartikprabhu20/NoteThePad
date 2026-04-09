package com.mintanable.notethepad.database.db.repository

import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.core.model.note.NoteOrder
import com.mintanable.notethepad.database.db.entity.NoteWithTags
import com.mintanable.notethepad.database.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

interface NoteRepository {
    val isSyncing: Flow<Boolean>
    fun getNotes(noteOrder: NoteOrder): Flow<List<NoteWithTags>>
    fun getNotesByIds(ids: List<String>): Flow<List<NoteWithTags>>
    suspend fun getNoteById(id: String): NoteWithTags?
    suspend fun insertNote(noteEntity: NoteEntity, tagEntities: List<TagEntity>): String
    suspend fun deleteNote(noteEntity: NoteEntity)
    suspend fun deleteNoteWithId(id: String)
    suspend fun restoreNote(id: String)
    suspend fun deleteNotePermanently(id: String)
    fun getDeletedNotes(): Flow<List<NoteWithTags>>
    suspend fun getNotesWithFutureReminders(currentTime: Long): List<NoteWithTags>
    fun getTopNotes(limit: Int): Flow<List<NoteWithTags>>
    fun getAllTags(): Flow<List<TagEntity>>
    suspend fun insertTag(tagEntity: TagEntity): Long
    suspend fun updateTag(tagEntity: TagEntity)
    suspend fun deleteTag(tagEntity: TagEntity)
    suspend fun getTagByName(tagName: String): TagEntity?
    suspend fun getTagById(id: String): TagEntity?
    suspend fun checkpoint(): File
    suspend fun swapDatabase(dbFile: File)
    suspend fun triggerSync()
    suspend fun fetchCloudData()
    suspend fun updateSummary(noteId: String, summary: String)
    suspend fun updateReminderTime(noteId: String, reminderTime: Long)
}