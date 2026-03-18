package com.mintanable.notethepad.feature_settings.presentation.util

import com.mintanable.notethepad.feature_settings.R


enum class PermissionRationaleType(val titleRes: Int, val messageRes: Int) {
    NOTIFICATION(R.string.permission_notification_title, R.string.permission_notification_msg),
    CAMERA(R.string.permission_camera_title, R.string.permission_camera_msg),
    MICROPHONE(R.string.permission_microphone_title, R.string.permission_microphone_msg),
    CAMERA_DENIED(R.string.permission_camera_denied_title, R.string.permission_camera_denied_msg),
    MICROPHONE_DENIED(R.string.permission_microphone_denied_title, R.string.permission_microphone_denied_msg),
}

enum class DeniedType { CAMERA, MICROPHONE }