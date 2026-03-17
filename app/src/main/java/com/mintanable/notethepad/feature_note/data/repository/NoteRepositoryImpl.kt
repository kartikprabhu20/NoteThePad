package com.mintanable.notethepad.feature_note.data.repository

import androidx.room.withTransaction
import com.mintanable.notethepad.core.model.Note
import com.mintanable.notethepad.core.model.NoteTagCrossRef
import com.mintanable.notethepad.core.model.NoteWithTags
import com.mintanable.notethepad.core.model.Tag
import com.mintanable.notethepad.feature_note.data.source.NoteDao
import com.mintanable.notethepad.feature_note.data.source.NoteDatabase
import com.mintanable.notethepad.feature_note.data.source.TagDao
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
                var noteId = noteDao.inserNote(note)

                if (noteId == -1L || noteId == 0L) { // Room returns 0 or -1 depending on setup for failed insert
                    noteDao.updateNote(note)
                    noteId = note.id
                }

                noteDao.deleteLinksForNote(noteId)
                tags.forEach { tag ->
                    var tagId = tagDao.insertTag(tag)

                    if (tagId == -1L || tagId == 0L) {
                         val existingTag = tagDao.getTagByName(tag.tagName)
                         tagId = existingTag?.tagId ?: 0L
                    }

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

    override suspend fun insertTag(tag: Tag): Long {
        return tagDao.insertTag(tag)
    }

    override suspend fun updateTag(tag: Tag) {
        tagDao.updateTag(tag)
    }

    override suspend fun deleteTag(tag: Tag) {
        tagDao.deleteTag(tag)
    }

    override suspend fun getTagByName(tagName: String): Tag? {
        return tagDao.getTagByName(tagName)
    }
}