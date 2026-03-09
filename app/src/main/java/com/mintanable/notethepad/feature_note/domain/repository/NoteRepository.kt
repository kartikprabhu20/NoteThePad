package com.mintanable.notethepad.feature_note.domain.repository

import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.model.NoteWithTags
import com.mintanable.notethepad.feature_note.domain.model.Tag
import com.mintanable.notethepad.feature_note.domain.util.NoteOrder
import kotlinx.coroutines.flow.Flow

interface NoteRepository {

    fun getNotes(noteOrder: NoteOrder): Flow<List<NoteWithTags>>

    suspend fun getNoteById(id:Long):NoteWithTags?

    suspend fun insertNote(note:Note, tags: List<Tag>): Long

    suspend fun deleteNote(note:Note)

    suspend fun deleteNoteWithId(id:Long)

    suspend fun getNotesWithFutureReminders(currentTime: Long): List<NoteWithTags>

    fun getTopNotes(limit: Int): Flow<List<NoteWithTags>>

    fun getAllTags() : Flow<List<Tag>>

    suspend fun insertTag(tag: Tag)
}