package com.mintanable.notethepad.feature_widgets.di

import com.mintanable.notethepad.core.common.WidgetRefresher
import com.mintanable.notethepad.feature_widgets.GlanceWidgetRefresher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {

    @Binds
    @Singleton
    abstract fun bindWidgetRefresher(impl: GlanceWidgetRefresher): WidgetRefresher
}
