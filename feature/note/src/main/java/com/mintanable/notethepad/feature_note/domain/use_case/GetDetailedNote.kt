package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.core.model.note.DetailedNote
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.database.helper.DetailedNoteMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetDetailedNote(
    private val repository: NoteRepository,
    private val detailedNoteMapper: DetailedNoteMapper
) {
    suspend operator fun invoke(id: Long): DetailedNote? = withContext(Dispatchers.IO) {
        val noteWithTags = repository.getNoteById(id) ?: return@withContext null
        return@withContext detailedNoteMapper.toDetailedNote(noteWithTags.note, noteWithTags.tags)
    }
}
