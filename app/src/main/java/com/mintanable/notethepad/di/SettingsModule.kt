package com.mintanable.notethepad.di

import com.mintanable.notethepad.feature_settings.data.repository.SharedPreferencesRepository
import com.mintanable.notethepad.feature_settings.presentation.use_cases.GetCameraPermissionFlag
import com.mintanable.notethepad.feature_settings.presentation.use_cases.GetMicrophonePermissionFlag
import com.mintanable.notethepad.feature_settings.presentation.use_cases.MarkCameraPermissionFlag
import com.mintanable.notethepad.feature_settings.presentation.use_cases.MarkMicrophonePermissionFlag
import com.mintanable.notethepad.feature_settings.presentation.use_cases.PermissionUsecases
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun providePermissionUseCases(repository: SharedPreferencesRepository): PermissionUsecases {
        return PermissionUsecases(
            markCameraPermissionFlag = MarkCameraPermissionFlag(repository),
            getCameraPermissionFlag = GetCameraPermissionFlag(repository),
            markMicrophonePermissionFlag = MarkMicrophonePermissionFlag(repository),
            getMicrophonePermissionFlag = GetMicrophonePermissionFlag(repository)
        )
    }
}