package com.mintanable.notethepad.database.db.repository

import androidx.room.withTransaction
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.core.model.note.NoteOrder
import com.mintanable.notethepad.database.db.entity.NoteTagCrossRef
import com.mintanable.notethepad.database.db.entity.NoteWithTags
import com.mintanable.notethepad.core.model.note.OrderType
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.database.db.NoteDatabase
import com.mintanable.notethepad.database.db.dao.NoteDao
import com.mintanable.notethepad.database.db.dao.TagDao
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

    override suspend fun insertNote(noteEntity: NoteEntity, tagEntities: List<TagEntity>): Long {
        return withContext(Dispatchers.IO) {
            db.withTransaction {
                var noteId = noteDao.inserNote(noteEntity)

                if (noteId == -1L || noteId == 0L) {
                    noteDao.updateNote(noteEntity)
                    noteId = noteEntity.id
                }

                noteDao.deleteLinksForNote(noteId)
                tagEntities.forEach { tag ->
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

    override suspend fun deleteNote(noteEntity: NoteEntity) {
        noteDao.deleteNote(noteEntity)
    }

    override suspend fun deleteNoteWithId(id: Long) {
        noteDao.deleteNoteWithId(id)
    }

    override suspend fun getNotesWithFutureReminders(currentTime: Long): List<NoteWithTags> {
        return noteDao.getNotesWithFutureReminders(currentTime)
    }

    override fun getTopNotes(limit: Int): Flow<List<NoteWithTags>> {
        return noteDao.getTopNotes(limit)
    }

    override fun getAllTags(): Flow<List<TagEntity>> {
        return tagDao.getAllTags()
    }

    override suspend fun insertTag(tagEntity: TagEntity): Long {
        return tagDao.insertTag(tagEntity)
    }

    override suspend fun updateTag(tagEntity: TagEntity) {
        tagDao.updateTag(tagEntity)
    }

    override suspend fun deleteTag(tagEntity: TagEntity) {
        tagDao.deleteTag(tagEntity)
    }

    override suspend fun getTagByName(tagName: String): TagEntity? {
        return tagDao.getTagByName(tagName)
    }
}
