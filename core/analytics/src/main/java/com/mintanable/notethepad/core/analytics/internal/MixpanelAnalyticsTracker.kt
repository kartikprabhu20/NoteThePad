package com.mintanable.notethepad.core.analytics.internal

import android.util.Log
import com.mintanable.notethepad.core.analytics.AnalyticsEvent
import com.mintanable.notethepad.core.analytics.AnalyticsTracker
import com.mintanable.notethepad.core.common.DispatcherProvider
import com.mixpanel.android.mpmetrics.MixpanelAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONObject

internal class MixpanelAnalyticsTracker(
    private val mixpanel: MixpanelAPI,
    private val preferences: AnalyticsPreferences,
    dispatchers: DispatcherProvider,
) : AnalyticsTracker {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val enabledState = MutableStateFlow(true)

    init {
        scope.launch {
            val deviceId = preferences.getDeviceId()
            mixpanel.identify(deviceId)
            mixpanel.people.set(PROPERTY_DEVICE_ID, deviceId)
        }
        scope.launch {
            preferences.analyticsEnabledFlow
                .onEach { enabledState.value = it }
                .collect { enabled ->
                    if (!enabled) mixpanel.optOutTracking() else mixpanel.optInTracking()
                }
        }
    }

    override fun track(event: AnalyticsEvent) {
        if (!enabledState.value) return
        runCatching {
            mixpanel.track(event.name, event.props.toJson())
        }.onFailure { Log.w(TAG, "track ${event.name} failed", it) }
    }

    override fun screenView(screen: String) {
        if (!enabledState.value) return
        runCatching {
            mixpanel.track(EVENT_SCREEN_VIEW, JSONObject().put(PARAM_SCREEN, screen))
        }.onFailure { Log.w(TAG, "screen_view $screen failed", it) }
    }

    override fun setUserProperty(key: String, value: Any) {
        if (!enabledState.value) return
        runCatching { mixpanel.people.set(key, value) }
            .onFailure { Log.w(TAG, "setUserProperty $key failed", it) }
    }

    override fun setEnabled(enabled: Boolean) {
        scope.launch { preferences.setEnabled(enabled) }
    }

    override fun flush() {
        runCatching { mixpanel.flush() }
    }

    private fun Map<String, Any?>.toJson(): JSONObject {
        val json = JSONObject()
        forEach { (k, v) -> if (v != null) json.put(k, v) }
        return json
    }

    private companion object {
        const val TAG = "MixpanelTracker"
        const val EVENT_SCREEN_VIEW = "screen_view"
        const val PARAM_SCREEN = "screen"
        const val PROPERTY_DEVICE_ID = "device_id"
    }
}
