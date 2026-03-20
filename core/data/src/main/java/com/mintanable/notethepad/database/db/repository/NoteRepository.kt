package com.mintanable.notethepad.database.db.repository

import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.core.model.note.NoteOrder
import com.mintanable.notethepad.database.db.entity.NoteWithTags
import com.mintanable.notethepad.database.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getNotes(noteOrder: NoteOrder): Flow<List<NoteWithTags>>
    suspend fun getNoteById(id: Long): NoteWithTags?
    suspend fun insertNote(noteEntity: NoteEntity, tagEntities: List<TagEntity>): Long
    suspend fun deleteNote(noteEntity: NoteEntity)
    suspend fun deleteNoteWithId(id: Long)
    suspend fun getNotesWithFutureReminders(currentTime: Long): List<NoteWithTags>
    fun getTopNotes(limit: Int): Flow<List<NoteWithTags>>
    fun getAllTags(): Flow<List<TagEntity>>
    suspend fun insertTag(tagEntity: TagEntity): Long
    suspend fun updateTag(tagEntity: TagEntity)
    suspend fun deleteTag(tagEntity: TagEntity)
    suspend fun getTagByName(tagName: String): TagEntity?
}
