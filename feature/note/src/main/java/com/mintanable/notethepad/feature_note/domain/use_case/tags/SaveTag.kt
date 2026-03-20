package com.mintanable.notethepad.feature_note.domain.use_case.tags

import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.database.db.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveTag(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(tagEntity: TagEntity): Long = withContext(Dispatchers.IO) {
        var tagId = repository.insertTag(tagEntity)

        if (tagId == -1L) {
            repository.updateTag(tagEntity)
            tagId = repository.getTagByName(tagEntity.tagName)?.tagId ?: tagId
        }
        return@withContext tagId
    }
}
