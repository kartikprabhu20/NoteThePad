package com.mintanable.notethepad.core.analytics.di

import android.content.Context
import com.mintanable.notethepad.core.analytics.AnalyticsTracker
import com.mintanable.notethepad.core.analytics.BuildConfig
import com.mintanable.notethepad.core.analytics.internal.AnalyticsPreferences
import com.mintanable.notethepad.core.analytics.internal.MixpanelAnalyticsTracker
import com.mintanable.notethepad.core.analytics.internal.NoopAnalyticsTracker
import com.mintanable.notethepad.core.common.DispatcherProvider
import com.mixpanel.android.mpmetrics.MixpanelAPI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsPreferences(
        @ApplicationContext context: Context,
    ): AnalyticsPreferences = AnalyticsPreferences(context)

    @Provides
    @Singleton
    fun provideAnalyticsTracker(
        @ApplicationContext context: Context,
        preferences: AnalyticsPreferences,
        dispatchers: DispatcherProvider,
    ): AnalyticsTracker {
        val token = BuildConfig.MIXPANEL_TOKEN
        if (token.isBlank()) return NoopAnalyticsTracker()
        val mixpanel = MixpanelAPI.getInstance(context, token, /* trackAutomaticEvents = */ true)
        return MixpanelAnalyticsTracker(mixpanel, preferences, dispatchers)
    }
}
