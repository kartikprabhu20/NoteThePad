package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.feature_note.domain.model.DetailedNote
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.util.DetailedNoteMapper
import com.mintanable.notethepad.feature_note.domain.util.NoteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class GetDetailedNotes(
    private val repository: NoteRepository,
    private val detailedNoteMapper: DetailedNoteMapper
) {
    operator fun invoke(order: NoteOrder): Flow<List<DetailedNote>> {
        return repository.getNotes(order).map { notesList ->
            coroutineScope {
                notesList.map { note ->
                    async { detailedNoteMapper.toDetailedNote(note) }
                }.awaitAll()
            }
        }.flowOn(Dispatchers.Default)
    }
}