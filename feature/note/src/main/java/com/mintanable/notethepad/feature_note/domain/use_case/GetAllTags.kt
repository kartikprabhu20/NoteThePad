package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.core.model.Tag
import com.mintanable.notethepad.database.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class GetAllTags(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<Tag>> {
        return repository.getAllTags()
    }
}
