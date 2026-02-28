package com.mintanable.notethepad.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.mintanable.notethepad.core.file.FileManager
import com.mintanable.notethepad.feature_firebase.data.repository.AuthRepositoryImpl
import com.mintanable.notethepad.feature_firebase.domain.repository.AuthRepository
import com.mintanable.notethepad.feature_navigationdrawer.data.repository.NavigationDrawerItemRepositoryImpl
import com.mintanable.notethepad.feature_navigationdrawer.domain.repository.NavigationDrawerItemRepository
import com.mintanable.notethepad.feature_navigationdrawer.domain.usecase.GetNavigationDrawerItems
import com.mintanable.notethepad.feature_note.data.repository.AndroidMediaPlayer
import com.mintanable.notethepad.feature_note.data.repository.NoteRepositoryImpl
import com.mintanable.notethepad.feature_note.data.source.NoteDao
import com.mintanable.notethepad.feature_note.data.repository.AndroidAudioRecorder
import com.mintanable.notethepad.feature_note.data.repository.ReminderSchedulerImpl
import com.mintanable.notethepad.feature_note.domain.repository.MediaPlayer
import com.mintanable.notethepad.feature_note.domain.repository.AudioRecorder
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.repository.ReminderScheduler
import com.mintanable.notethepad.feature_note.domain.use_case.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideNoteRepository(dao: NoteDao): NoteRepository {
        return NoteRepositoryImpl(dao)
    }

    @Provides
    fun provideNoteUseCases(repository: NoteRepository, fileManager: FileManager, @ApplicationContext context: Context): NoteUseCases {
        return NoteUseCases(
            getNotes = GetNotes(repository),
            deleteNote = DeleteNote(repository),
            saveNoteWithAttachments = SaveNoteWithAttachments(repository, fileManager, context),
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
    fun provideNavigationDrawerRepository(): NavigationDrawerItemRepository {
        return NavigationDrawerItemRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(auth: FirebaseAuth): AuthRepository {
        return AuthRepositoryImpl(auth)
    }

    @Provides
    @Singleton
    fun provideAudioRecorder(@ApplicationContext context: Context): AudioRecorder {
        return AndroidAudioRecorder(context)
    }

    @Provides
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
    fun provideMediaPlayer(@ApplicationContext context: Context): MediaPlayer{
        return AndroidMediaPlayer(context)
    }

    @Provides
    @Singleton
    fun provideReminderScheduler(@ApplicationContext context: Context): ReminderScheduler{
        return ReminderSchedulerImpl(context)
    }
}