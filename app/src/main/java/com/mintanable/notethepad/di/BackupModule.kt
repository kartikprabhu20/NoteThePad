package com.mintanable.notethepad.di

import com.mintanable.notethepad.core.worker.BackupSchedulerImpl
import com.mintanable.notethepad.feature_backup.data.repository.GoogleAuthRepositoryImpl
import com.mintanable.notethepad.feature_backup.domain.BackupScheduler
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
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
    abstract fun bindBackupScheduler(
        impl: BackupSchedulerImpl
    ): BackupScheduler
}