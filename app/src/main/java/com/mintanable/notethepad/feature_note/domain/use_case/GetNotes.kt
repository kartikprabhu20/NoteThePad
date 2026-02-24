package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.util.NoteOrder
import kotlinx.coroutines.flow.Flow

class GetNotes(
    private val repository: NoteRepository
    ) {

    operator fun invoke(order: NoteOrder): Flow<List<Note>>{
        return repository.getNotes(order)
    }
}