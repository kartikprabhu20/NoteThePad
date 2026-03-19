package com.mintanable.notethepad.feature_note.di

import android.content.Context
import com.mintanable.notethepad.file.FileManager
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.feature_note.data.repository.AndroidAudioRecorder
import com.mintanable.notethepad.feature_note.data.repository.AndroidMediaPlayer
import com.mintanable.notethepad.feature_note.data.repository.ReminderSchedulerImpl
import com.mintanable.notethepad.feature_note.domain.repository.AudioRecorder
import com.mintanable.notethepad.feature_note.domain.repository.MediaPlayer
import com.mintanable.notethepad.feature_note.domain.repository.ReminderScheduler
import com.mintanable.notethepad.feature_note.domain.use_case.CreateFile
import com.mintanable.notethepad.feature_note.domain.use_case.CreateUri
import com.mintanable.notethepad.feature_note.domain.use_case.DeleteFiles
import com.mintanable.notethepad.feature_note.domain.use_case.DeleteNote
import com.mintanable.notethepad.feature_note.domain.use_case.FileIOUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.GetAllTags
import com.mintanable.notethepad.feature_note.domain.use_case.GetDetailedNote
import com.mintanable.notethepad.feature_note.domain.use_case.GetDetailedNotes
import com.mintanable.notethepad.feature_note.domain.use_case.GetNotesWithReminders
import com.mintanable.notethepad.feature_note.domain.use_case.GetNotesWithTag
import com.mintanable.notethepad.feature_note.domain.use_case.GetTopNotes
import com.mintanable.notethepad.feature_note.domain.use_case.NoteUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.SaveMediaToStorage
import com.mintanable.notethepad.feature_note.domain.use_case.SaveNoteWithAttachments
import com.mintanable.notethepad.feature_note.domain.use_case.SaveTag
import com.mintanable.notethepad.feature_note.domain.use_case.TagUseCases
import com.mintanable.notethepad.database.helper.DetailedNoteMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NoteModule {

    @Provides
    @Singleton
    fun provideNoteUseCases(
        repository: NoteRepository,
        fileManager: FileManager,
        detailedNoteMapper: DetailedNoteMapper,
        @ApplicationContext context: Context
    ): NoteUseCases {
        return NoteUseCases(
            getDetailedNotes = GetDetailedNotes(repository, detailedNoteMapper),
            getNotesWithReminders = GetNotesWithReminders(repository, detailedNoteMapper),
            getNotesWithTags = GetNotesWithTag(repository, detailedNoteMapper),
            deleteNote = DeleteNote(repository),
            saveNoteWithAttachments = SaveNoteWithAttachments(repository, fileManager, context),
            getDetailedNote = GetDetailedNote(repository, detailedNoteMapper),
            getTopNotes = GetTopNotes(repository, detailedNoteMapper)
        )
    }

    @Provides
    @Singleton
    fun provideTagUseCases(
        repository: NoteRepository
    ): TagUseCases {
        return TagUseCases(
            getAllTags = GetAllTags(repository),
            deleteTag = com.mintanable.notethepad.feature_note.domain.use_case.DeleteTag(repository),
            saveTag = SaveTag(repository)
        )
    }

    @Provides
    @Singleton
    fun provideAudioRecorder(@ApplicationContext context: Context): AudioRecorder {
        return AndroidAudioRecorder(context)
    }

    @Provides
    @Singleton
    fun provideMediaPlayer(@ApplicationContext context: Context): MediaPlayer {
        return AndroidMediaPlayer(context)
    }

    @Provides
    @Singleton
    fun provideReminderScheduler(@ApplicationContext context: Context): ReminderScheduler {
        return ReminderSchedulerImpl(context)
    }

    @Provides
    @Singleton
    fun provideFileIOUseCases(fileManager: FileManager): FileIOUseCases {
        return FileIOUseCases(
            createFile = CreateFile(fileManager),
            createUri = CreateUri(fileManager),
            deleteFiles = DeleteFiles(fileManager),
            saveMediaToStorage = SaveMediaToStorage(fileManager)
        )
    }
}
