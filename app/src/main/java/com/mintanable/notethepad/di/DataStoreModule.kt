package com.mintanable.notethepad.di

import android.content.Context
import com.mintanable.notethepad.feature_settings.data.repository.UserPreferencesRepository
import com.mintanable.notethepad.feature_settings.domain.use_case.GetLayoutSettings
import com.mintanable.notethepad.feature_settings.domain.use_case.ToggleLayoutSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        @ApplicationContext context: Context
    ): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideGetLayoutSettings(repository: UserPreferencesRepository): GetLayoutSettings {
        return GetLayoutSettings(repository)
    }

    @Provides
    @Singleton
    fun provideToggleLayoutSettings(repository: UserPreferencesRepository): ToggleLayoutSettings {
        return ToggleLayoutSettings(repository)
    }
}