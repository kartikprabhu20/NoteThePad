package com.mintanable.notethepad.feature_settings.di

import com.mintanable.notethepad.feature_settings.domain.use_case.GetCameraPermissionFlag
import com.mintanable.notethepad.feature_settings.domain.use_case.GetMicrophonePermissionFlag
import com.mintanable.notethepad.feature_settings.domain.use_case.MarkCameraPermissionFlag
import com.mintanable.notethepad.feature_settings.domain.use_case.MarkMicrophonePermissionFlag
import com.mintanable.notethepad.feature_settings.domain.use_case.PermissionUsecases
import com.mintanable.notethepad.database.preference.repository.SharedPreferencesRepository
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
