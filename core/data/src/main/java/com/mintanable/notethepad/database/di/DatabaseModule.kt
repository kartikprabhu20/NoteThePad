package com.mintanable.notethepad.database.di

import android.content.Context
import com.mintanable.notethepad.core.common.DispatcherProvider
import com.mintanable.notethepad.database.db.DatabaseManager
import com.mintanable.notethepad.database.db.NoteDatabase
import com.mintanable.notethepad.database.db.dao.NoteDao
import com.mintanable.notethepad.database.db.dao.TagDao
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.database.db.repository.NoteRepositoryImpl
import com.mintanable.notethepad.database.db.util.AudioMetadataProvider
import com.mintanable.notethepad.database.helper.DetailedNoteMapper
import com.mintanable.notethepad.database.preference.repository.SharedPreferencesRepository
import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabaseManager(@ApplicationContext context: Context): DatabaseManager {
        return DatabaseManager(context)
    }

    @Provides
    @Singleton
    fun provideNoteDatabase(manager: DatabaseManager): NoteDatabase {
        return manager.database
    }

    @Provides
    @Singleton
    fun provideNoteDao(db: NoteDatabase): NoteDao {
        return db.noteDao
    }

    @Provides
    @Singleton
    fun provideTagDao(db: NoteDatabase): TagDao = db.tagDao

    @Provides
    @Singleton
    fun provideNoteRepository(noteDao: NoteDao, tagDao: TagDao, noteDatabase: NoteDatabase): NoteRepository {
        return NoteRepositoryImpl(noteDao, tagDao, noteDatabase)
    }

    @Provides
    @Singleton
    fun provideDetailedNoteMapper(audioMetadataProvider: AudioMetadataProvider, dispatchers: DispatcherProvider): DetailedNoteMapper {
        return DetailedNoteMapper(audioMetadataProvider, dispatchers)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideSharedPreferencesRepository(@ApplicationContext context: Context): SharedPreferencesRepository {
        return SharedPreferencesRepository(context)
    }
}