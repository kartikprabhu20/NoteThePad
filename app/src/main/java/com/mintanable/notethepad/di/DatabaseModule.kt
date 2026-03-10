package com.mintanable.notethepad.di

import android.content.Context
import com.mintanable.notethepad.core.file.FileManager
import com.mintanable.notethepad.feature_note.data.repository.NoteRepositoryImpl
import com.mintanable.notethepad.feature_note.data.source.DatabaseManager
import com.mintanable.notethepad.feature_note.data.source.NoteDao
import com.mintanable.notethepad.feature_note.data.source.NoteDatabase
import com.mintanable.notethepad.feature_note.data.source.TagDao
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.use_case.DeleteNote
import com.mintanable.notethepad.feature_note.domain.use_case.DeleteTag
import com.mintanable.notethepad.feature_note.domain.use_case.GetAllTags
import com.mintanable.notethepad.feature_note.domain.use_case.GetDetailedNote
import com.mintanable.notethepad.feature_note.domain.use_case.GetDetailedNotes
import com.mintanable.notethepad.feature_note.domain.use_case.GetNotesWithReminders
import com.mintanable.notethepad.feature_note.domain.use_case.GetNotesWithTag
import com.mintanable.notethepad.feature_note.domain.use_case.GetTopNotes
import com.mintanable.notethepad.feature_note.domain.use_case.NoteUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.SaveNoteWithAttachments
import com.mintanable.notethepad.feature_note.domain.use_case.SaveTag
import com.mintanable.notethepad.feature_note.domain.use_case.TagUseCases
import com.mintanable.notethepad.feature_note.domain.util.DetailedNoteMapper
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
    fun provideNoteUseCases(
        repository: NoteRepository,
        fileManager: FileManager,
        detailedNoteMapper: DetailedNoteMapper,
        @ApplicationContext context: Context): NoteUseCases {
        return NoteUseCases(
            getDetailedNotes = GetDetailedNotes(repository, detailedNoteMapper),
            getNotesWithReminders = GetNotesWithReminders(repository, detailedNoteMapper),
            getNotesWithTags =  GetNotesWithTag(repository, detailedNoteMapper),
            deleteNote = DeleteNote(repository),
            saveNoteWithAttachments = SaveNoteWithAttachments(repository, fileManager, context),
            getDetailedNote = GetDetailedNote(repository, detailedNoteMapper),
            getTopNotes = GetTopNotes(repository, detailedNoteMapper)
        )
    }

    @Provides
    fun provideTagUseCases(
        repository: NoteRepository
    ) : TagUseCases {
        return TagUseCases(
            getAllTags = GetAllTags(repository),
            deleteTag = DeleteTag(repository),
            saveTag = SaveTag(repository)
        )
    }

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
}