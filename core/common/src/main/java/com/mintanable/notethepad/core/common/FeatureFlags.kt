package com.mintanable.notethepad.core.common

object FeatureFlags {
    var collaborationEnabled: Boolean = true
        private set
    var aiAssistanceEnabled: Boolean = true
        private set

    fun init(collaboration: Boolean, aiAssistance: Boolean) {
        collaborationEnabled = collaboration
        aiAssistanceEnabled = aiAssistance
    }
}
