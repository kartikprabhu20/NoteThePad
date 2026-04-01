package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.database.db.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RefreshSupaSync @Inject constructor(
    private val repository: NoteRepository
) {
     suspend operator fun invoke() = withContext(Dispatchers.IO){
         repository.triggerSync()
         repository.fetchCloudData()
    }
}
