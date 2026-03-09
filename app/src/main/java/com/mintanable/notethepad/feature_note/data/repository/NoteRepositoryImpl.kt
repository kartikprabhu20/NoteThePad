package com.mintanable.notethepad.feature_note.data.repository

import androidx.room.withTransaction
import com.mintanable.notethepad.feature_note.data.source.NoteDao
import com.mintanable.notethepad.feature_note.data.source.NoteDatabase
import com.mintanable.notethepad.feature_note.data.source.TagDao
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.model.NoteTagCrossRef
import com.mintanable.notethepad.feature_note.domain.model.NoteWithTags
import com.mintanable.notethepad.feature_note.domain.model.Tag
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.util.NoteOrder
import com.mintanable.notethepad.feature_note.domain.util.OrderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val db: NoteDatabase
) : NoteRepository {

    override fun getNotes(noteOrder: NoteOrder): Flow<List<NoteWithTags>> {
        return noteDao.getNotes(
            order = when(noteOrder) {
                is NoteOrder.Title -> "title"
                is NoteOrder.Date -> "date"
                is NoteOrder.Color -> "color"
            },
            ascending = if(noteOrder.orderType == OrderType.Ascending) 1 else 0
        )
    }

    override suspend fun getNoteById(id: Long): NoteWithTags? {
        return noteDao.getNoteById(id)
    }

    override suspend fun insertNote(note: Note, tags: List<Tag>) : Long {
        return withContext(Dispatchers.IO) {
            db.withTransaction {
                val noteId = noteDao.inserNote(note)
                noteDao.deleteLinksForNote(noteId)
                tags.forEach { tag ->
                    val tagId = tagDao.insertTag(tag)
                    noteDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
                }
                noteId
            }
        }
    }

    override suspend fun deleteNote(note: Note) {
        return noteDao.deleteNote(note)
    }

    override suspend fun deleteNoteWithId(id: Long) {
        return noteDao.deleteNoteWithId(id)
    }

    override suspend fun getNotesWithFutureReminders(currentTime: Long) : List<NoteWithTags> {
        return noteDao.getNotesWithFutureReminders(currentTime)
    }

    override fun getTopNotes(limit: Int): Flow<List<NoteWithTags>> {
        return noteDao.getTopNotes(limit)
    }

    override fun getAllTags(): Flow<List<Tag>> {
        return tagDao.getAllTags()
    }

    override suspend fun insertTag(tag: Tag) {
        tagDao.insertTag(tag)
    }
}