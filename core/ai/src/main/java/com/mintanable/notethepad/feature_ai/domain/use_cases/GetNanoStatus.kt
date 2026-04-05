package com.mintanable.notethepad.feature_ai.domain.use_cases

import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import javax.inject.Inject

/**
 * Reports whether Gemini Nano (ML Kit on-device GenAI) is usable on the current device.
 *
 * Delegates to `NoteAssistantRepository.checkLocalStatus("Gemini Nano (System)")` which in
 * turn calls `Generation.getClient().checkStatus()`. Returns [AiModelDownloadStatus.Unavailable]
 * on devices that don't support AICore.
 */
class GetNanoStatus @Inject constructor(
    private val noteAssistantRepository: NoteAssistantRepository
) {
    suspend operator fun invoke() =
        noteAssistantRepository.checkLocalStatus("Gemini Nano (System)")
}
