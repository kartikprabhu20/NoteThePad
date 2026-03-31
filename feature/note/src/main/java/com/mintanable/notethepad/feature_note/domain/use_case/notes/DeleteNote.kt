package com.mintanable.notethepad.feature_note.domain.use_case.notes

import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.database.db.repository.NoteRepository

class DeleteNote(
    private val repository: NoteRepository
) {

    suspend operator fun invoke(noteEntity: NoteEntity){
        repository.deleteNote(noteEntity)
    }

    suspend operator fun invoke(id: String){
        repository.deleteNoteWithId(id)
    }
}
