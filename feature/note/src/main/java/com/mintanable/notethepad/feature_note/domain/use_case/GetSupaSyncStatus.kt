package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.database.db.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class GetSupaSyncStatus @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<Boolean> {
         return repository.isSyncing
             .distinctUntilChanged()
    }
}
