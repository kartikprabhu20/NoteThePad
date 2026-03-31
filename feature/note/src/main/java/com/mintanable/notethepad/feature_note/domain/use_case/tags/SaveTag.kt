package com.mintanable.notethepad.feature_note.domain.use_case.tags

import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.database.db.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveTag(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(tagEntity: TagEntity): String = withContext(Dispatchers.IO) {
        var rowId = repository.insertTag(tagEntity)
        var tagId = tagEntity.tagId

        if (rowId == -1L) {
            repository.updateTag(tagEntity)
            tagId = repository.getTagByName(tagEntity.tagName)?.tagId  ?: tagId
        }
        return@withContext tagId
    }
}
