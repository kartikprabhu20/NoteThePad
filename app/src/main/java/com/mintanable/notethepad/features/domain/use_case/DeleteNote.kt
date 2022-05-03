package com.mintanable.notethepad.features.domain.use_case

import com.mintanable.notethepad.features.domain.model.Note
import com.mintanable.notethepad.features.domain.repository.NoteRepository

class DeleteNote(
    private val repository: NoteRepository
) {

    suspend operator fun invoke(note: Note){
        repository.deleteNote(note)
    }
}