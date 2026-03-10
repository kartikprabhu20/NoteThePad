package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.feature_note.domain.model.Tag
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveTag(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(tag: Tag): Long = withContext(Dispatchers.IO){
        val existingTagId = repository.getTagByName(tag.tagName)?.tagId

        return@withContext existingTagId ?: repository.insertTag(tag)
    }
}
