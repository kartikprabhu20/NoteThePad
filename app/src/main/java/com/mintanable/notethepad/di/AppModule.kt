package com.mintanable.notethepad.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.mintanable.notethepad.core.file.FileManager
import com.mintanable.notethepad.feature_firebase.data.repository.AuthRepositoryImpl
import com.mintanable.notethepad.feature_firebase.domain.repository.AuthRepository
import com.mintanable.notethepad.feature_navigationdrawer.data.repository.NavigationDrawerItemRepositoryImpl
import com.mintanable.notethepad.feature_navigationdrawer.domain.repository.NavigationDrawerItemRepository
import com.mintanable.notethepad.feature_navigationdrawer.domain.usecase.GetNavigationDrawerItems
import com.mintanable.notethepad.feature_note.data.repository.AndroidAudioRecorder
import com.mintanable.notethepad.feature_note.data.repository.AndroidMediaPlayer
import com.mintanable.notethepad.feature_note.data.repository.ReminderSchedulerImpl
import com.mintanable.notethepad.feature_note.domain.repository.AudioRecorder
import com.mintanable.notethepad.feature_note.domain.repository.MediaPlayer
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.repository.ReminderScheduler
import com.mintanable.notethepad.feature_note.domain.use_case.CreateFile
import com.mintanable.notethepad.feature_note.domain.use_case.CreateUri
import com.mintanable.notethepad.feature_note.domain.use_case.DeleteFiles
import com.mintanable.notethepad.feature_note.domain.use_case.FileIOUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.SaveMediaToStorage
import com.mintanable.notethepad.feature_note.domain.util.DefaultDispatcherProvider
import com.mintanable.notethepad.feature_note.domain.util.DispatcherProvider
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
    @Singleton
    fun provideGetNavigationDrawerItemsUseCase(repository: NavigationDrawerItemRepository, noteRepository: NoteRepository): GetNavigationDrawerItems {
        return GetNavigationDrawerItems(repository, noteRepository)
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

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

}