package com.mintanable.notethepad.features.domain.use_case

import com.mintanable.notethepad.features.domain.model.Note
import com.mintanable.notethepad.features.domain.repository.NoteRepository

class GetNote(
    private val repository: NoteRepository
) {

    suspend operator fun invoke(id:Int): Note?{
        return repository.getNoteById(id)
    }
}