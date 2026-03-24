package com.mintanable.notethepad.feature_ai.domain.use_cases

import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import javax.inject.Inject

class StopLiveTranscription @Inject constructor(
    private val noteAssistantRepository: NoteAssistantRepository
){
    suspend operator fun invoke(){
        noteAssistantRepository.stopLiveTranscription()
    }
}