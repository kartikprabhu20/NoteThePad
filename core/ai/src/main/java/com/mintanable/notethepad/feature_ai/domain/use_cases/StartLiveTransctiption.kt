package com.mintanable.notethepad.feature_ai.domain.use_cases

import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class StartLiveTransctiption @Inject constructor(
    private val noteAssistantRepository: NoteAssistantRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    suspend operator fun invoke(onTranscription: (String) -> Unit) {
        val settings = userPreferencesRepository.settingsFlow.first()
        noteAssistantRepository.startLiveTranscription(onTranscription, settings.aiModelName)
    }
}