package com.mintanable.notethepad.feature_note.domain.use_case.tags

import com.mintanable.notethepad.core.model.note.TagEntity
import com.mintanable.notethepad.database.db.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class GetAllTags(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<TagEntity>> {
        return repository.getAllTags()
    }
}
