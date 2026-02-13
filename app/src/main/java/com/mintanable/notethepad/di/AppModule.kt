package com.mintanable.notethepad.di

import android.app.Application
import androidx.room.Room
import com.mintanable.notethepad.feature_navigationdrawer.data.repository.NavigationDrawerItemRepositoryImpl
import com.mintanable.notethepad.feature_navigationdrawer.domain.repository.NavigationDrawerItemRepository
import com.mintanable.notethepad.feature_navigationdrawer.domain.usecase.GetNavigationDrawerItems
import com.mintanable.notethepad.features.data.repository.NoteRepositoryImpl
import com.mintanable.notethepad.features.data.source.NoteDao
import com.mintanable.notethepad.features.data.source.NoteDatabase
import com.mintanable.notethepad.features.domain.repository.NoteRepository
import com.mintanable.notethepad.features.domain.use_case.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideNoteDatabase(app:Application):NoteDatabase{
        return Room.databaseBuilder(
            app,
            NoteDatabase::class.java,
            NoteDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideNoteRepository(db: NoteDatabase): NoteRepository{
        return NoteRepositoryImpl(db.noteDao)
    }

    @Provides
    @Singleton
    fun provideNoteUseCases(repository: NoteRepository):NoteUseCases {
        return NoteUseCases(
            getNotes = GetNotes(repository),
            deleteNote = DeleteNote(repository),
            addNote = AddNote(repository),
            getNote = GetNote(repository)
        )
    }

    @Provides
    @Singleton
    fun provideGetNavigationDrawerItemsUseCase(repository: NavigationDrawerItemRepository): GetNavigationDrawerItems {
        return GetNavigationDrawerItems(repository)
    }

    @Provides
    @Singleton
    fun provideNavigationDrawerRepository(): NavigationDrawerItemRepository{
        return NavigationDrawerItemRepositoryImpl()
    }
}