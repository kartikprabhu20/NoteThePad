package com.mintanable.notethepad.core.analytics

sealed class AnalyticsEvent(
    val name: String,
    val props: Map<String, Any?>,
) {
    // region AI
    class AiAutoTagTriggered(model: String, contentLen: Int) : AnalyticsEvent(
        Event.AI_AUTO_TAG_TRIGGERED,
        mapOf(Param.MODEL to model, Param.CONTENT_LEN to contentLen),
    )

    class AiAutoTagResult(success: Boolean, tagCount: Int, errorType: String? = null) : AnalyticsEvent(
        Event.AI_AUTO_TAG_RESULT,
        mapOf(Param.SUCCESS to success, Param.TAG_COUNT to tagCount, Param.ERROR_TYPE to errorType),
    )

    class AiSummarizeTriggered(model: String, contentLen: Int, attachmentCount: Int) : AnalyticsEvent(
        Event.AI_SUMMARIZE_TRIGGERED,
        mapOf(
            Param.MODEL to model,
            Param.CONTENT_LEN to contentLen,
            Param.ATTACHMENT_COUNT to attachmentCount,
        ),
    )

    class AiSummarizeResult(success: Boolean, summaryLen: Int, durationMs: Long) : AnalyticsEvent(
        Event.AI_SUMMARIZE_RESULT,
        mapOf(
            Param.SUCCESS to success,
            Param.SUMMARY_LEN to summaryLen,
            Param.DURATION_MS to durationMs,
        ),
    )

    class AiImageAnalyzeTriggered(imageBytes: Int, model: String) : AnalyticsEvent(
        Event.AI_IMAGE_ANALYZE_TRIGGERED,
        mapOf(Param.IMAGE_BYTES to imageBytes, Param.MODEL to model),
    )

    class AiImageAnalyzeResult(success: Boolean, suggestionCount: Int) : AnalyticsEvent(
        Event.AI_IMAGE_ANALYZE_RESULT,
        mapOf(Param.SUCCESS to success, Param.SUGGESTION_COUNT to suggestionCount),
    )

    class AiImageQuerySubmitted(queryLen: Int, model: String) : AnalyticsEvent(
        Event.AI_IMAGE_QUERY_SUBMITTED,
        mapOf(Param.QUERY_LEN to queryLen, Param.MODEL to model),
    )

    class AiImageQueryResult(success: Boolean, chunkCount: Int, resultLen: Int) : AnalyticsEvent(
        Event.AI_IMAGE_QUERY_RESULT,
        mapOf(
            Param.SUCCESS to success,
            Param.CHUNK_COUNT to chunkCount,
            Param.RESULT_LEN to resultLen,
        ),
    )

    class AiAudioTranscribeTriggered(audioDurationMs: Long, model: String) : AnalyticsEvent(
        Event.AI_AUDIO_TRANSCRIBE_TRIGGERED,
        mapOf(Param.AUDIO_DURATION_MS to audioDurationMs, Param.MODEL to model),
    )

    class AiLiveTranscriptionToggled(enabled: Boolean) : AnalyticsEvent(
        Event.AI_LIVE_TRANSCRIPTION_TOGGLED,
        mapOf(Param.ENABLED to enabled),
    )

    class AiModelSelected(model: String) : AnalyticsEvent(
        Event.AI_MODEL_SELECTED,
        mapOf(Param.MODEL to model),
    )

    class AiModelDownloadStarted(model: String, sizeBytes: Long) : AnalyticsEvent(
        Event.AI_MODEL_DOWNLOAD_STARTED,
        mapOf(Param.MODEL to model, Param.SIZE_BYTES to sizeBytes),
    )

    class AiModelDownloadResult(model: String, success: Boolean, durationMs: Long, errorType: String? = null) : AnalyticsEvent(
        Event.AI_MODEL_DOWNLOAD_RESULT,
        mapOf(Param.MODEL to model, Param.SUCCESS to success, Param.DURATION_MS to durationMs, Param.ERROR_TYPE to errorType),
    )
    // endregion

    // region Attachments
    class AttachmentAdded(type: String, source: String) : AnalyticsEvent(
        Event.ATTACHMENT_ADDED,
        mapOf(Param.TYPE to type, Param.SOURCE to source),
    )

    class AttachmentRemoved(type: String) : AnalyticsEvent(
        Event.ATTACHMENT_REMOVED,
        mapOf(Param.TYPE to type),
    )

    class AttachmentAudioRecorded(durationMs: Long) : AnalyticsEvent(
        Event.ATTACHMENT_AUDIO_RECORDED,
        mapOf(Param.DURATION_MS to durationMs),
    )

    // endregion

    // region Theme
    class ThemeChanged(mode: String, previousMode: String) : AnalyticsEvent(
        Event.THEME_CHANGED,
        mapOf(Param.MODE to mode, Param.PREVIOUS_MODE to previousMode),
    )
    // endregion

    // region Backup
    class BackupSettingsChanged(frequency: String, backupMedia: Boolean) : AnalyticsEvent(
        Event.BACKUP_SETTINGS_CHANGED,
        mapOf(Param.FREQUENCY to frequency, Param.BACKUP_MEDIA to backupMedia),
    )

    class BackupNowRequested(mediaIncluded: Boolean) : AnalyticsEvent(
        Event.BACKUP_NOW_REQUESTED,
        mapOf(Param.MEDIA_INCLUDED to mediaIncluded),
    )

    class BackupStarted(trigger: String) : AnalyticsEvent(
        Event.BACKUP_STARTED,
        mapOf(Param.TRIGGER to trigger),
    )

    class BackupResult(
        success: Boolean,
        durationMs: Long,
        attempt: Int,
        errorType: String? = null,
    ) : AnalyticsEvent(
        Event.BACKUP_RESULT,
        mapOf(
            Param.SUCCESS to success,
            Param.DURATION_MS to durationMs,
            Param.ATTEMPT to attempt,
            Param.ERROR_TYPE to errorType,
        ),
    )

    object RestoreStarted : AnalyticsEvent(Event.RESTORE_STARTED, emptyMap())

    class RestoreResult(
        success: Boolean,
        durationMs: Long,
        errorType: String? = null,
    ) : AnalyticsEvent(
        Event.RESTORE_RESULT,
        mapOf(
            Param.SUCCESS to success,
            Param.DURATION_MS to durationMs,
            Param.ERROR_TYPE to errorType,
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
        Event.NOTE_SHARED,
        mapOf(
            Param.HAS_ATTACHMENTS to hasAttachments,
            Param.IMAGE_COUNT to imageCount,
            Param.AUDIO_COUNT to audioCount,
            Param.TAG_COUNT to tagCount,
        ),
    )

    class CollaboratorInvited(success: Boolean) : AnalyticsEvent(
        Event.COLLABORATOR_INVITED,
        mapOf(Param.SUCCESS to success),
    )

    object CollaboratorRemoved : AnalyticsEvent(Event.COLLABORATOR_REMOVED, emptyMap())
    // endregion

    // region Rich text
    class RichTextFormatApplied(spanType: String) : AnalyticsEvent(
        Event.RICHTEXT_FORMAT_APPLIED,
        mapOf(Param.SPAN_TYPE to spanType),
    )

    class RichTextBarToggled(shown: Boolean) : AnalyticsEvent(
        Event.RICHTEXT_BAR_TOGGLED,
        mapOf(Param.SHOWN to shown),
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
        Event.NOTE_CREATED,
        mapOf(
            Param.HAS_ATTACHMENTS to hasAttachments,
            Param.ATTACHMENT_COUNT to attachmentCount,
            Param.TAG_COUNT to tagCount,
            Param.CONTENT_LEN to contentLen,
            Param.IS_CHECKLIST to isChecklist,
        ),
    )

    class NoteUpdated(
        hasAttachments: Boolean,
        attachmentCount: Int,
        tagCount: Int,
        contentLen: Int,
        isChecklist: Boolean,
    ) : AnalyticsEvent(
        Event.NOTE_UPDATED,
        mapOf(
            Param.HAS_ATTACHMENTS to hasAttachments,
            Param.ATTACHMENT_COUNT to attachmentCount,
            Param.TAG_COUNT to tagCount,
            Param.CONTENT_LEN to contentLen,
            Param.IS_CHECKLIST to isChecklist,
        ),
    )

    class NoteDeleted(count: Int = 1) : AnalyticsEvent(
        Event.NOTE_DELETED,
        mapOf(Param.COUNT to count),
    )

    class NotePinned(pinned: Boolean) : AnalyticsEvent(
        Event.NOTE_PINNED,
        mapOf(Param.PINNED to pinned),
    )

    class NoteArchived(archived: Boolean) : AnalyticsEvent(
        Event.NOTE_ARCHIVED,
        mapOf(Param.ARCHIVED to archived),
    )

    class NoteUndoRedo(action: String) : AnalyticsEvent(
        Event.NOTE_UNDO_REDO,
        mapOf(Param.UNDO_REDO to action),
    )

    // endregion

    // region Auth
    class AuthSignInAttempted(method: String) : AnalyticsEvent(
        Event.AUTH_SIGN_IN_ATTEMPTED,
        mapOf(Param.METHOD to method),
    )

    class AuthSignInResult(success: Boolean, errorType: String? = null) : AnalyticsEvent(
        Event.AUTH_SIGN_IN_RESULT,
        mapOf(Param.SUCCESS to success, Param.ERROR_TYPE to errorType),
    )

    object AuthSignOut : AnalyticsEvent(Event.AUTH_SIGN_OUT, emptyMap())
 
    class PermissionDenied(permission: String) : AnalyticsEvent(
        Event.PERMISSION_DENIED,
        mapOf(Param.PERMISSION_NAME to permission),
    )
    // endregion

    companion object {
        object Event {
            const val AI_AUTO_TAG_TRIGGERED = "ai_auto_tag_triggered"
            const val AI_AUTO_TAG_RESULT = "ai_auto_tag_result"
            const val AI_SUMMARIZE_TRIGGERED = "ai_summarize_triggered"
            const val AI_SUMMARIZE_RESULT = "ai_summarize_result"
            const val AI_IMAGE_ANALYZE_TRIGGERED = "ai_image_analyze_triggered"
            const val AI_IMAGE_ANALYZE_RESULT = "ai_image_analyze_result"
            const val AI_IMAGE_QUERY_SUBMITTED = "ai_image_query_submitted"
            const val AI_IMAGE_QUERY_RESULT = "ai_image_query_result"
            const val AI_AUDIO_TRANSCRIBE_TRIGGERED = "ai_audio_transcribe_triggered"
            const val AI_LIVE_TRANSCRIPTION_TOGGLED = "ai_live_transcription_toggled"
            const val AI_MODEL_SELECTED = "ai_model_selected"
            const val AI_MODEL_DOWNLOAD_STARTED = "ai_model_download_started"
            const val AI_MODEL_DOWNLOAD_RESULT = "ai_model_download_result"
            const val ATTACHMENT_ADDED = "attachment_added"
            const val ATTACHMENT_REMOVED = "attachment_removed"
            const val ATTACHMENT_AUDIO_RECORDED = "attachment_audio_recorded"
            const val ATTACHMENT_IMAGE_ZOOMED = "attachment_image_zoomed"
            const val THEME_CHANGED = "theme_changed"
            const val BACKUP_SETTINGS_CHANGED = "backup_settings_changed"
            const val BACKUP_NOW_REQUESTED = "backup_now_requested"
            const val BACKUP_STARTED = "backup_started"
            const val BACKUP_RESULT = "backup_result"
            const val RESTORE_STARTED = "restore_started"
            const val RESTORE_RESULT = "restore_result"
            const val NOTE_SHARED = "note_shared"
            const val COLLABORATOR_INVITED = "collaborator_invited"
            const val COLLABORATOR_REMOVED = "collaborator_removed"
            const val RICHTEXT_FORMAT_APPLIED = "richtext_format_applied"
            const val RICHTEXT_BAR_TOGGLED = "richtext_bar_toggled"
            const val NOTE_CREATED = "note_created"
            const val NOTE_UPDATED = "note_updated"
            const val NOTE_DELETED = "note_deleted"
            const val NOTE_PINNED = "note_pinned"
            const val NOTE_ARCHIVED = "note_archived"
            const val NOTE_UNDO_REDO = "note_undo_redo"
            const val AUTH_SIGN_IN_ATTEMPTED = "auth_sign_in_attempted"
            const val AUTH_SIGN_IN_RESULT = "auth_sign_in_result"
            const val AUTH_SIGN_OUT = "auth_sign_out"
            const val PERMISSION_DENIED = "permission_denied"
        }

        object Param {
            const val MODEL = "model"
            const val CONTENT_LEN = "content_len"
            const val SUCCESS = "success"
            const val TAG_COUNT = "tag_count"
            const val ERROR_TYPE = "error_type"
            const val ATTACHMENT_COUNT = "attachment_count"
            const val SUMMARY_LEN = "summary_len"
            const val DURATION_MS = "duration_ms"
            const val IMAGE_BYTES = "image_bytes"
            const val SUGGESTION_COUNT = "suggestion_count"
            const val QUERY_LEN = "query_len"
            const val CHUNK_COUNT = "chunk_count"
            const val RESULT_LEN = "result_len"
            const val AUDIO_DURATION_MS = "audio_duration_ms"
            const val ENABLED = "enabled"
            const val TYPE = "type"
            const val SOURCE = "source"
            const val HAS_TRANSCRIPTION = "has_transcription"
            const val HAS_SUMMARY = "has_summary"
            const val MODE = "mode"
            const val PREVIOUS_MODE = "previous_mode"
            const val FREQUENCY = "frequency"
            const val BACKUP_MEDIA = "backup_media"
            const val MEDIA_INCLUDED = "media_included"
            const val TRIGGER = "trigger"
            const val ATTEMPT = "attempt"
            const val HAS_ATTACHMENTS = "has_attachments"
            const val IMAGE_COUNT = "image_count"
            const val AUDIO_COUNT = "audio_count"
            const val SPAN_TYPE = "span_type"
            const val SHOWN = "shown"
            const val IS_CHECKLIST = "is_checklist"
            const val COUNT = "count"
            const val PINNED = "pinned"
            const val ARCHIVED = "archived"
            const val UNDO_REDO = "undo_redo"
            const val METHOD = "method"
            const val SIZE_BYTES = "size_bytes"
            const val PERMISSION_NAME = "permission_name"
        }
    }
}