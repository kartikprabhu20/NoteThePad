package com.mintanable.notethepad.feature_note.domain.repository

import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.util.NoteOrder
import kotlinx.coroutines.flow.Flow

interface NoteRepository {

    fun getNotes(noteOrder: NoteOrder): Flow<List<Note>>

    suspend fun getNoteById(id:Long):Note?

    suspend fun insertNote(note:Note): Long

    suspend fun deleteNote(note:Note)

    suspend fun deleteNoteWithId(id:Long)

    suspend fun getNotesWithFutureReminders(currentTime: Long): List<Note>

}