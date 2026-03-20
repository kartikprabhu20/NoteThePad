package com.mintanable.notethepad.feature_note.domain.use_case.permissions

import com.mintanable.notethepad.database.preference.repository.SharedPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetCameraPermissionFlag @Inject constructor(
    private val repository: SharedPreferencesRepository
) {
    suspend operator fun invoke(): Boolean = repository.askedCameraPermission.first()
}
