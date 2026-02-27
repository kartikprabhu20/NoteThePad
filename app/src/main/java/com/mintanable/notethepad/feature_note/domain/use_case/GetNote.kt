package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository

class GetNote(
    private val repository: NoteRepository
) {

    suspend operator fun invoke(id:Long): Note?{
        return repository.getNoteById(id)
    }
}