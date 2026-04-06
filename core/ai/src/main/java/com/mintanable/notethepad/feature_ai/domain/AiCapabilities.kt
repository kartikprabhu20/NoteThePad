package com.mintanable.notethepad.feature_ai.domain

import com.mintanable.notethepad.core.model.ai.AiCapabilities
import com.mintanable.notethepad.core.model.ai.AiModel

/**
 * Maps an [AiModel] to the set of magic-button features it can power.
 *
 * This mirrors the routing branches in `NoteAssistantRepositoryImpl` — if a new
 * model family is added there, this function must be updated too.
 */
fun AiModel?.toCapabilities(): AiCapabilities {
    if (this == null) return AiCapabilities.NONE
    return when (name) {
        "None" -> AiCapabilities.NONE
        "Gemini 3 Flash (Cloud)" -> AiCapabilities(
            canAutoTag = true,
            canAnalyzeImage = true,
            canTranscribeAudio = false, // not implemented for cloud
            isLiveTranscriptionSupported = false,
        )
        "Gemini Nano (System)" -> AiCapabilities(
            canAutoTag = true,
            canAnalyzeImage = false,    // Nano doesn't accept images
            canTranscribeAudio = true,  // gated to Android 12+ at callsite
            isLiveTranscriptionSupported = true,
        )
        else -> AiCapabilities(
            // Gemma-family local LLMs — read metadata from catalog
            canAutoTag = isLlm,
            canAnalyzeImage = llmSupportImage,
            canTranscribeAudio = llmSupportAudio,
            isLiveTranscriptionSupported = false,
        )
    }
}
