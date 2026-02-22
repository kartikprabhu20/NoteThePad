package com.mintanable.notethepad.di

import android.content.Context
import androidx.work.WorkManager
import com.mintanable.notethepad.core.worker.BackupSchedulerImpl
import com.mintanable.notethepad.feature_backup.data.repository.GoogleAuthRepositoryImpl
import com.mintanable.notethepad.feature_backup.data.repository.GoogleDriveRepositoryImpl
import com.mintanable.notethepad.feature_backup.domain.BackupScheduler
import com.mintanable.notethepad.feature_backup.domain.GoogleDriveService
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleAuthRepository
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleDriveRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {

    @Binds
    @Singleton
    abstract fun bindGoogleAuthRepository(
        impl: GoogleAuthRepositoryImpl
    ): GoogleAuthRepository

    @Binds
    @Singleton
    abstract fun bindGoogleDriveRepository(
        impl: GoogleDriveRepositoryImpl
    ): GoogleDriveRepository

    @Binds
    @Singleton
    abstract fun bindBackupScheduler(
        impl: BackupSchedulerImpl
    ): BackupScheduler

    companion object {
        @Provides
        @Singleton
        fun provideWorkManager(
            @ApplicationContext context: Context
        ): WorkManager {
            return WorkManager.getInstance(context)
        }

        @Provides
        @Singleton
        fun provideGoogleDriveService(): GoogleDriveService {
            return GoogleDriveService()
        }
    }
}