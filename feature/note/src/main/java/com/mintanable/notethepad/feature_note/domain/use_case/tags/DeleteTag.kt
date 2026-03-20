package com.mintanable.notethepad.feature_note.domain.use_case.tags

import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.database.db.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteTag(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(tagEntity: TagEntity) = withContext(Dispatchers.IO){
        repository.deleteTag(tagEntity)
    }
}
