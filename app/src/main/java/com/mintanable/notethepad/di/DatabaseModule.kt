package com.mintanable.notethepad.di

import android.content.Context
import com.mintanable.notethepad.feature_note.data.source.DatabaseManager
import com.mintanable.notethepad.feature_note.data.source.NoteDao
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
    fun provideNoteDao(manager: DatabaseManager): NoteDao {
        return manager.database.noteDao
    }
}