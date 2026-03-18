package com.mintanable.notethepad.feature_backup.presentation

sealed class RestoreEvent {
    object NavigateToHome : RestoreEvent()
}