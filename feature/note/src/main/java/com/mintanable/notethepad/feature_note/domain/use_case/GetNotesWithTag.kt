package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.core.model.DetailedNote
import com.mintanable.notethepad.core.model.Tag
import com.mintanable.notethepad.core.model.NoteOrder
import com.mintanable.notethepad.database.repository.NoteRepository
import com.mintanable.notethepad.helper.DetailedNoteMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class GetNotesWithTag(
    private val repository: NoteRepository,
    private val detailedNoteMapper: DetailedNoteMapper
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(order: NoteOrder, tag: Tag): Flow<List<DetailedNote>> {
        return repository.getNotes(order).mapLatest { notesList ->
            notesList.map { noteWithTags ->
                detailedNoteMapper.toDetailedNote(noteWithTags.note, noteWithTags.tags)
            }.filter { note ->
                note.tags.map { it.tagName }.contains(tag.tagName)
            }
        }
    }
}
