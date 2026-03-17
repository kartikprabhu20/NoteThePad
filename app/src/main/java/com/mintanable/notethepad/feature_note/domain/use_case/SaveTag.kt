package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.core.model.Tag
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveTag(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(tag: Tag): Long = withContext(Dispatchers.IO){
        var tagId = repository.insertTag(tag)

        if (tagId == -1L) {
            repository.updateTag(tag)
            tagId = repository.getTagByName(tag.tagName)?.tagId ?: tagId
        }
        return@withContext tagId
    }
}