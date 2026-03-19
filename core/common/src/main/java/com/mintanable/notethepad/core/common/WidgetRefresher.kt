package com.mintanable.notethepad.core.common

import android.content.Context

interface WidgetRefresher {
    suspend fun refresh(context: Context)
}
