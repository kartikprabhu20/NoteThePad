package com.mintanable.notethepad.core.analytics

sealed class AnalyticsEvent(
    val name: String,
    val props: Map<String, Any?>,
) {
    // region AI
    class AiAutoTagTriggered(model: String, contentLen: Int) : AnalyticsEvent(
        "ai_auto_tag_triggered",
        mapOf("model" to model, "content_len" to contentLen),
    )

    class AiAutoTagResult(success: Boolean, tagCount: Int, errorType: String? = null) : AnalyticsEvent(
        "ai_auto_tag_result",
        mapOf("success" to success, "tag_count" to tagCount, "error_type" to errorType),
    )

    class AiSummarizeTriggered(model: String, contentLen: Int, attachmentCount: Int) : AnalyticsEvent(
        "ai_summarize_triggered",
        mapOf(
            "model" to model,
            "content_len" to contentLen,
            "attachment_count" to attachmentCount,
        ),
    )

    class AiSummarizeResult(success: Boolean, summaryLen: Int, durationMs: Long) : AnalyticsEvent(
        "ai_summarize_result",
        mapOf(
            "success" to success,
            "summary_len" to summaryLen,
            "duration_ms" to durationMs,
        ),
    )

    class AiImageAnalyzeTriggered(imageBytes: Int) : AnalyticsEvent(
        "ai_image_analyze_triggered",
        mapOf("image_bytes" to imageBytes),
    )

    class AiImageAnalyzeResult(success: Boolean, suggestionCount: Int) : AnalyticsEvent(
        "ai_image_analyze_result",
        mapOf("success" to success, "suggestion_count" to suggestionCount),
    )

    class AiImageQuerySubmitted(queryLen: Int) : AnalyticsEvent(
        "ai_image_query_submitted",
        mapOf("query_len" to queryLen),
    )

    class AiImageQueryResult(success: Boolean, chunkCount: Int, resultLen: Int) : AnalyticsEvent(
        "ai_image_query_result",
        mapOf(
            "success" to success,
            "chunk_count" to chunkCount,
            "result_len" to resultLen,
        ),
    )

    class AiAudioTranscribeTriggered(audioDurationMs: Long) : AnalyticsEvent(
        "ai_audio_transcribe_triggered",
        mapOf("audio_duration_ms" to audioDurationMs),
    )

    class AiLiveTranscriptionToggled(enabled: Boolean) : AnalyticsEvent(
        "ai_live_transcription_toggled",
        mapOf("enabled" to enabled),
    )

    class AiModelSelected(model: String) : AnalyticsEvent(
        "ai_model_selected",
        mapOf("model" to model),
    )
    // endregion

    // region Attachments
    class AttachmentAdded(type: String, source: String) : AnalyticsEvent(
        "attachment_added",
        mapOf("type" to type, "source" to source),
    )

    class AttachmentRemoved(type: String) : AnalyticsEvent(
        "attachment_removed",
        mapOf("type" to type),
    )

    class AttachmentAudioRecorded(durationMs: Long) : AnalyticsEvent(
        "attachment_audio_recorded",
        mapOf("duration_ms" to durationMs),
    )

    class AttachmentImageZoomed(hasTranscription: Boolean, hasSummary: Boolean) : AnalyticsEvent(
        "attachment_image_zoomed",
        mapOf("has_transcription" to hasTranscription, "has_summary" to hasSummary),
    )
    // endregion

    // region Theme
    class ThemeChanged(mode: String, previousMode: String) : AnalyticsEvent(
        "theme_changed",
        mapOf("mode" to mode, "previous_mode" to previousMode),
    )
    // endregion

    // region Backup
    class BackupSettingsChanged(frequency: String, backupMedia: Boolean) : AnalyticsEvent(
        "backup_settings_changed",
        mapOf("frequency" to frequency, "backup_media" to backupMedia),
    )

    class BackupNowRequested(mediaIncluded: Boolean) : AnalyticsEvent(
        "backup_now_requested",
        mapOf("media_included" to mediaIncluded),
    )

    class BackupStarted(trigger: String) : AnalyticsEvent(
        "backup_started",
        mapOf("trigger" to trigger),
    )

    class BackupResult(
        success: Boolean,
        durationMs: Long,
        attempt: Int,
        errorType: String? = null,
    ) : AnalyticsEvent(
        "backup_result",
        mapOf(
            "success" to success,
            "duration_ms" to durationMs,
            "attempt" to attempt,
            "error_type" to errorType,
        ),
    )

    object RestoreStarted : AnalyticsEvent("restore_started", emptyMap())

    class RestoreResult(
        success: Boolean,
        durationMs: Long,
        errorType: String? = null,
    ) : AnalyticsEvent(
        "restore_result",
        mapOf(
            "success" to success,
            "duration_ms" to durationMs,
            "error_type" to errorType,
        ),
    )
    // endregion

    // region Sharing / Collaboration
    class NoteShared(
        hasAttachments: Boolean,
        imageCount: Int,
        audioCount: Int,
        tagCount: Int,
    ) : AnalyticsEvent(
        "note_shared",
        mapOf(
            "has_attachments" to hasAttachments,
            "image_count" to imageCount,
            "audio_count" to audioCount,
            "tag_count" to tagCount,
        ),
    )

    class CollaboratorInvited(success: Boolean) : AnalyticsEvent(
        "collaborator_invited",
        mapOf("success" to success),
    )

    object CollaboratorRemoved : AnalyticsEvent("collaborator_removed", emptyMap())
    // endregion

    // region Rich text
    class RichTextFormatApplied(spanType: String) : AnalyticsEvent(
        "richtext_format_applied",
        mapOf("span_type" to spanType),
    )

    class RichTextBarToggled(shown: Boolean) : AnalyticsEvent(
        "richtext_bar_toggled",
        mapOf("shown" to shown),
    )
    // endregion

    // region Note lifecycle
    class NoteCreated(
        hasAttachments: Boolean,
        attachmentCount: Int,
        tagCount: Int,
        contentLen: Int,
        isChecklist: Boolean,
    ) : AnalyticsEvent(
        "note_created",
        mapOf(
            "has_attachments" to hasAttachments,
            "attachment_count" to attachmentCount,
            "tag_count" to tagCount,
            "content_len" to contentLen,
            "is_checklist" to isChecklist,
        ),
    )

    class NoteUpdated(
        hasAttachments: Boolean,
        attachmentCount: Int,
        tagCount: Int,
        contentLen: Int,
        isChecklist: Boolean,
    ) : AnalyticsEvent(
        "note_updated",
        mapOf(
            "has_attachments" to hasAttachments,
            "attachment_count" to attachmentCount,
            "tag_count" to tagCount,
            "content_len" to contentLen,
            "is_checklist" to isChecklist,
        ),
    )

    class NoteDeleted(count: Int = 1) : AnalyticsEvent(
        "note_deleted",
        mapOf("count" to count),
    )

    class NotePinned(pinned: Boolean) : AnalyticsEvent(
        "note_pinned",
        mapOf("pinned" to pinned),
    )

    class NoteArchived(archived: Boolean) : AnalyticsEvent(
        "note_archived",
        mapOf("archived" to archived),
    )
    // endregion

    // region Auth
    class AuthSignInAttempted(method: String) : AnalyticsEvent(
        "auth_sign_in_attempted",
        mapOf("method" to method),
    )

    class AuthSignInResult(success: Boolean, errorType: String? = null) : AnalyticsEvent(
        "auth_sign_in_result",
        mapOf("success" to success, "error_type" to errorType),
    )

    object AuthSignOut : AnalyticsEvent("auth_sign_out", emptyMap())
    // endregion
}
