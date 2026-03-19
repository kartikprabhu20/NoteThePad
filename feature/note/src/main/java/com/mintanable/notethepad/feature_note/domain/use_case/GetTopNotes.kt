package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.core.model.DetailedNote
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.database.helper.DetailedNoteMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class GetTopNotes(
    private val repository: NoteRepository,
    private val detailedNoteMapper: DetailedNoteMapper
) {
    operator fun invoke(limit: Int): Flow<List<DetailedNote>> {
        return repository.getTopNotes(limit).map { notesList ->
            coroutineScope {
                notesList.map { noteWithTags ->
                    async { detailedNoteMapper.toDetailedNote(noteWithTags.note, noteWithTags.tags) }
                }.awaitAll()
            }
        }.flowOn(Dispatchers.Default)
    }
}
