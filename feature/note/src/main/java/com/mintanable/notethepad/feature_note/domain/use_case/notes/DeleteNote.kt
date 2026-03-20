package com.mintanable.notethepad.feature_note.domain.use_case.notes

import com.mintanable.notethepad.core.model.note.NoteEntity
import com.mintanable.notethepad.database.db.repository.NoteRepository

class DeleteNote(
    private val repository: NoteRepository
) {

    suspend operator fun invoke(noteEntity: NoteEntity){
        repository.deleteNote(noteEntity)
    }

    suspend operator fun invoke(id: Long){
        repository.deleteNoteWithId(id)
    }
}
