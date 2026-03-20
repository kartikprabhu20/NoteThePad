package com.mintanable.notethepad.feature_note.di

import android.content.Context
import com.mintanable.notethepad.database.db.repository.NavigationDrawerItemRepository
import com.mintanable.notethepad.file.FileManager
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.feature_note.data.repository.AndroidAudioRecorder
import com.mintanable.notethepad.feature_note.data.repository.AndroidMediaPlayer
import com.mintanable.notethepad.feature_note.data.repository.ReminderSchedulerImpl
import com.mintanable.notethepad.feature_note.domain.repository.AudioRecorder
import com.mintanable.notethepad.feature_note.domain.repository.MediaPlayer
import com.mintanable.notethepad.feature_note.domain.repository.ReminderScheduler
import com.mintanable.notethepad.feature_note.domain.use_case.fileio.CreateFile
import com.mintanable.notethepad.feature_note.domain.use_case.fileio.CreateUri
import com.mintanable.notethepad.feature_note.domain.use_case.fileio.DeleteFiles
import com.mintanable.notethepad.feature_note.domain.use_case.notes.DeleteNote
import com.mintanable.notethepad.feature_note.domain.use_case.fileio.FileIOUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.tags.GetAllTags
import com.mintanable.notethepad.feature_note.domain.use_case.notes.GetDetailedNote
import com.mintanable.notethepad.feature_note.domain.use_case.notes.GetDetailedNotes
import com.mintanable.notethepad.feature_note.domain.use_case.notes.GetNotesWithReminders
import com.mintanable.notethepad.feature_note.domain.use_case.notes.GetNotesWithTag
import com.mintanable.notethepad.feature_note.domain.use_case.notes.GetTopNotes
import com.mintanable.notethepad.feature_note.domain.use_case.notes.NoteUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.fileio.SaveMediaToStorage
import com.mintanable.notethepad.feature_note.domain.use_case.notes.SaveNoteWithAttachments
import com.mintanable.notethepad.feature_note.domain.use_case.tags.SaveTag
import com.mintanable.notethepad.feature_note.domain.use_case.tags.TagUseCases
import com.mintanable.notethepad.database.helper.DetailedNoteMapper
import com.mintanable.notethepad.database.preference.repository.SharedPreferencesRepository
import com.mintanable.notethepad.feature_note.domain.use_case.GetNavigationDrawerItems
import com.mintanable.notethepad.feature_note.domain.use_case.permissions.GetCameraPermissionFlag
import com.mintanable.notethepad.feature_note.domain.use_case.permissions.GetMicrophonePermissionFlag
import com.mintanable.notethepad.feature_note.domain.use_case.permissions.MarkCameraPermissionFlag
import com.mintanable.notethepad.feature_note.domain.use_case.permissions.MarkMicrophonePermissionFlag
import com.mintanable.notethepad.feature_note.domain.use_case.permissions.PermissionUsecases
import com.mintanable.notethepad.feature_note.domain.use_case.tags.DeleteTag
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
        detailedNoteMapper: DetailedNoteMapper
    ): NoteUseCases {
        return NoteUseCases(
            getDetailedNotes = GetDetailedNotes(repository, detailedNoteMapper),
            getNotesWithReminders = GetNotesWithReminders(repository, detailedNoteMapper),
            getNotesWithTags = GetNotesWithTag(repository, detailedNoteMapper),
            deleteNote = DeleteNote(repository),
            saveNoteWithAttachments = SaveNoteWithAttachments(repository, fileManager),
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
            deleteTag = DeleteTag(repository),
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

    @Provides
    @Singleton
    fun provideGetNavigationDrawerItemsUseCase(
        repository: NavigationDrawerItemRepository,
        noteRepository: NoteRepository
    ): GetNavigationDrawerItems {
        return GetNavigationDrawerItems(repository, noteRepository)
    }

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
