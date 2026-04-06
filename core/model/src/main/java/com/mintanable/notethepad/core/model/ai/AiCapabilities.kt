package com.mintanable.notethepad.core.model.ai

/**
 * Feature gates derived from the currently selected [AiModel].
 *
 * Used by the note editor to show/hide the three "magic" buttons
 * (Auto-tag, Analyse Image, Transcribe) depending on what the model
 * can actually do.
 *
 * This class is data-only; Compose stability is declared externally via
 * `compose_stability.conf` (same as other :core:model DTOs).
 */
data class AiCapabilities(
    val canAutoTag: Boolean,
    val canAnalyzeImage: Boolean,
    val canTranscribeAudio: Boolean,
    val isLiveTranscriptionSupported: Boolean,
) {
    companion object {
        val NONE = AiCapabilities(
            canAutoTag = false,
            canAnalyzeImage = false,
            canTranscribeAudio = false,
            isLiveTranscriptionSupported = false,
        )
    }
}
