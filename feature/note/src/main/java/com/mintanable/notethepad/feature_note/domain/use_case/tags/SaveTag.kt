package com.mintanable.notethepad.feature_note.domain.use_case.tags

import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.database.db.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveTag(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(tagEntity: TagEntity): String = withContext(Dispatchers.IO) {
       val existingById = repository.getTagById(tagEntity.tagId)
        if (existingById != null) {
            repository.updateTag(
                existingById.copy(
                    tagName = tagEntity.tagName,
                    lastUpdateTime = System.currentTimeMillis(),
                    isDeleted = false
                )
            )
            return@withContext existingById.tagId
        }

        val existingByName = repository.getTagByName(tagEntity.tagName)
        if (existingByName != null) {
            return@withContext existingByName.tagId
        }

        repository.insertTag(tagEntity)
        return@withContext tagEntity.tagId
    }
}
