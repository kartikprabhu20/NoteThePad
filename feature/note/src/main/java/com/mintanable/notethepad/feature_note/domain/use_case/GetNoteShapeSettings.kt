package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.core.model.settings.NoteShape
import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetNoteShapeSettings @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<NoteShape> {
        return repository.settingsFlow
            .map { it.noteShape }
            .distinctUntilChanged()
    }
}
