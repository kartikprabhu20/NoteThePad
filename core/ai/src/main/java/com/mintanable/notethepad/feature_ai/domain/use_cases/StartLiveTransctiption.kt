package com.mintanable.notethepad.feature_ai.domain.use_cases

import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import javax.inject.Inject

class StartLiveTransctiption @Inject constructor(
    private val noteAssistantRepository: NoteAssistantRepository
) {

    suspend operator fun invoke(onTranscription: (String) -> Unit) {
        noteAssistantRepository.startLiveTranscription(onTranscription)
    }
}