package com.mintanable.notethepad.feature_settings.presentation

import androidx.annotation.StringRes
import com.mintanable.notethepad.feature_settings.R

data class HelpItem(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val stepsRes: Int
)

val helpItems: List<HelpItem> = listOf(
    HelpItem("create_note", R.string.help_item_create_note_title, R.string.help_item_create_note_steps),
    HelpItem("checklist", R.string.help_item_checklist_title, R.string.help_item_checklist_steps),
    HelpItem("rich_text", R.string.help_item_rich_text_title, R.string.help_item_rich_text_steps),
    HelpItem("attachments", R.string.help_item_attachments_title, R.string.help_item_attachments_steps),
    HelpItem("reminder", R.string.help_item_reminder_title, R.string.help_item_reminder_steps),
    HelpItem("share_note", R.string.help_item_share_note_title, R.string.help_item_share_note_steps),
    HelpItem("collaborate", R.string.help_item_collaborate_title, R.string.help_item_collaborate_steps),
//    HelpItem("ai_assistant", R.string.help_item_ai_assistant_title, R.string.help_item_ai_assistant_steps),
    HelpItem("tags", R.string.help_item_tags_title, R.string.help_item_tags_steps),
    HelpItem("export_pdf", R.string.help_item_export_pdf_title, R.string.help_item_export_pdf_steps),
    HelpItem("pin_widget", R.string.help_item_pin_widget_title, R.string.help_item_pin_widget_steps),
    HelpItem("backup_restore", R.string.help_item_backup_restore_title, R.string.help_item_backup_restore_steps),
    HelpItem("cloud_sync", R.string.help_item_cloud_sync_title, R.string.help_item_cloud_sync_steps),
)
