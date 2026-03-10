package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.feature_note.domain.model.DetailedNote
import com.mintanable.notethepad.feature_note.domain.model.Tag
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.util.DetailedNoteMapper
import com.mintanable.notethepad.feature_note.domain.util.NoteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

class GetNotesWithTag(
    private val repository: NoteRepository,
    private val detailedNoteMapper: DetailedNoteMapper
) {
    operator fun invoke(order: NoteOrder, tag: Tag): Flow<List<DetailedNote>> {
        return repository.getNotes(order).mapLatest { notesList ->
            notesList.map { noteWithTags ->
                detailedNoteMapper.toDetailedNote(noteWithTags.note, noteWithTags.tags)
            }.filter {  note ->
                note.tags.map { it.tagName }.contains(tag.tagName)
            }
        }
    }
}