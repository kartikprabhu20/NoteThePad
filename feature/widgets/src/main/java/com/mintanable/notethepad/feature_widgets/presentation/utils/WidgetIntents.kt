package com.mintanable.notethepad.feature_widgets.presentation.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.mintanable.notethepad.core.common.NavigationConstants

fun buildOpenNoteIntent(
    context: Context,
    noteId: String? = null,
    initialAction: String? = null,
    launchEditScreen: Boolean = initialAction != null
): Intent = Intent(NavigationConstants.ACTION_OPEN_NOTE).apply {
    component = ComponentName(context.packageName, NavigationConstants.MAIN_ACTIVITY_CLASS)
    if (!noteId.isNullOrBlank()) {
        putExtra(NavigationConstants.EXTRA_NOTE_ID, noteId)
    }
    if (launchEditScreen) {
        putExtra(NavigationConstants.LAUNCH_EDIT_SCREEN, true)
    }
    if (!initialAction.isNullOrBlank()) {
        putExtra(NavigationConstants.EXTRA_INITIAL_ACTION, initialAction)
    }
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
}
