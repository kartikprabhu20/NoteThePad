package com.mintanable.notethepad.core.analytics.internal

import com.mintanable.notethepad.core.analytics.AnalyticsEvent
import com.mintanable.notethepad.core.analytics.AnalyticsTracker

internal class NoopAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
    override fun screenView(screen: String) = Unit
    override fun setUserProperty(key: String, value: Any) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
    override fun flush() = Unit
}
