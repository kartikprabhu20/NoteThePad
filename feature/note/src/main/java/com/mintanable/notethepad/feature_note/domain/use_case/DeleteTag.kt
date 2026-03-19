package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.core.model.Tag
import com.mintanable.notethepad.database.db.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteTag(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(tag: Tag) = withContext(Dispatchers.IO){
        repository.deleteTag(tag)
    }
}
