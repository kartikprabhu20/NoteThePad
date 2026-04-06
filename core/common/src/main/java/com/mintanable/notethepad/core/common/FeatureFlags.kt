package com.mintanable.notethepad.core.common

object FeatureFlags {
    var collaborationEnabled: Boolean = false
        private set

    fun init(collaboration: Boolean) {
        collaborationEnabled = collaboration
    }
}
