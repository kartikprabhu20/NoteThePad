package com.mintanable.notethepad.feature_note.data.repository

import com.mintanable.notethepad.feature_note.data.source.NoteDao
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.util.NoteOrder
import com.mintanable.notethepad.feature_note.domain.util.OrderType
import kotlinx.coroutines.flow.Flow

class NoteRepositoryImpl(
    private val dao: NoteDao
) : NoteRepository {
    override fun getNotes(noteOrder: NoteOrder): Flow<List<Note>> {
        return dao.getNotes(
            order = when(noteOrder) {
                is NoteOrder.Title -> "title"
                is NoteOrder.Date -> "date"
                is NoteOrder.Color -> "color"
            },
            ascending = if(noteOrder.orderType == OrderType.Ascending) 1 else 0
        )
    }

    override suspend fun getNoteById(id: Long): Note? {
        return dao.getNoteById(id)
    }

    override suspend fun insertNote(note: Note) : Long {
        return dao.inserNote(note)
    }

    override suspend fun deleteNote(note: Note) {
        return dao.deleteNote(note)
    }
}