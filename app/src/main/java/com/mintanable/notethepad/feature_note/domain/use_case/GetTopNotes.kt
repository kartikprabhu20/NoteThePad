package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class GetTopNotes(
    private val repository: NoteRepository
) {
    operator fun invoke(limit: Int): Flow<List<Note>> {
        return repository.getTopNotes(limit)
    }
}
