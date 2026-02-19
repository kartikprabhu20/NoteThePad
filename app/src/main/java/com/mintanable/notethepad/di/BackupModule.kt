package com.mintanable.notethepad.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.mintanable.notethepad.core.security.CryptoManager
import com.mintanable.notethepad.feature_backup.data.repository.DriveRepositoryImpl
import com.mintanable.notethepad.feature_backup.domain.repository.DriveRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object BackupModule {

    @Provides
    @Singleton
    fun provideDriveRepository(
        cryptoManager: CryptoManager,
        dataStore: DataStore<Preferences>,
        @ApplicationContext context: Context
    ): DriveRepository {
        return DriveRepositoryImpl(cryptoManager, dataStore, context)
    }
}