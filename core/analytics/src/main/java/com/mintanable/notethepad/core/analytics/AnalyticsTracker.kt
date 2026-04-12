package com.mintanable.notethepad.core.analytics

interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
    fun screenView(screen: String)
    fun setUserProperty(key: String, value: Any)
    fun setEnabled(enabled: Boolean)
    fun flush()
}
