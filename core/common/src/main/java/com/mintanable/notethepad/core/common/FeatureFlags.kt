package com.mintanable.notethepad.core.common

object FeatureFlags {
    var collaborationEnabled: Boolean = true
        private set

    fun init(collaboration: Boolean) {
        collaborationEnabled = collaboration
    }
}
