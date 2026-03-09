package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.feature_note.domain.model.DetailedNote
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.util.DetailedNoteMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetDetailedNote(
    private val repository: NoteRepository,
    private val detailedNoteMapper: DetailedNoteMapper
) {

    suspend operator fun invoke(id:Long): DetailedNote? = withContext(Dispatchers.IO){
        val noteWithTags = repository.getNoteById(id) ?: return@withContext null
        return@withContext detailedNoteMapper.toDetailedNote(noteWithTags.note, noteWithTags.tags.map { it.tagName })
    }
}