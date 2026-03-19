package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.core.model.Note
import com.mintanable.notethepad.database.db.repository.NoteRepository

class DeleteNote(
    private val repository: NoteRepository
) {

    suspend operator fun invoke(note: Note){
        repository.deleteNote(note)
    }

    suspend operator fun invoke(id: Long){
        repository.deleteNoteWithId(id)
    }
}
