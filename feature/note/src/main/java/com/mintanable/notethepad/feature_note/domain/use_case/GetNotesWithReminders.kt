package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.core.model.note.DetailedNote
import com.mintanable.notethepad.core.model.note.NoteOrder
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.database.helper.DetailedNoteMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class GetNotesWithReminders(
    private val repository: NoteRepository,
    private val detailedNoteMapper: DetailedNoteMapper
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(order: NoteOrder): Flow<List<DetailedNote>> {
        return repository.getNotes(order).mapLatest { notesList ->
            notesList.map { noteWithTags ->
                detailedNoteMapper.toDetailedNote(noteWithTags.note, noteWithTags.tags)
            }.filter {
                it.reminderTime > 0
            }
        }
    }
}
