package com.mintanable.notethepad.feature_note.domain.use_case.notes

import com.mintanable.notethepad.database.db.entity.DetailedNote
import com.mintanable.notethepad.database.db.repository.CollaborationRepository
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.database.helper.DetailedNoteMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest

class GetSharedNotes(
    private val collaborationRepository: CollaborationRepository,
    private val noteRepository: NoteRepository,
    private val detailedNoteMapper: DetailedNoteMapper
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(userId: String): Flow<List<DetailedNote>> {
        return collaborationRepository.getSharedNoteIds(userId).flatMapLatest { ids ->
            noteRepository.getNotesByIds(ids).mapLatest { notesList ->
                notesList.map { noteWithTags ->
                    detailedNoteMapper.toDetailedNote(noteWithTags.noteEntity, noteWithTags.tagEntities)
                }
            }
        }
    }
}
