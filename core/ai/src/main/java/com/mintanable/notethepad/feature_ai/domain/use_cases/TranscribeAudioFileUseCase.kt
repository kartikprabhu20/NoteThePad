package com.mintanable.notethepad.feature_ai.domain.use_cases

import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class TranscribeAudioFileUseCase @Inject constructor(
    private val noteAssistantRepository: NoteAssistantRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(uri: String, onTranscription: (String) -> Unit){
        val settings = userPreferencesRepository.settingsFlow.first()
        noteAssistantRepository.transcribeAudioFile(uri, settings.aiModelName, onTranscription)
    }
}
