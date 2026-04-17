package com.mintanable.notethepad.database.di

import android.content.Context
import com.mintanable.notethepad.auth.repository.AuthRepository
import com.mintanable.notethepad.core.common.DispatcherProvider
import com.mintanable.notethepad.core.network.sync.SupabaseSyncService
import com.mintanable.notethepad.database.db.DatabaseManager
import com.mintanable.notethepad.database.db.NoteDatabase
import com.mintanable.notethepad.database.db.dao.CollaboratorDao
import com.mintanable.notethepad.database.db.dao.NoteDao
import com.mintanable.notethepad.database.db.dao.TagDao
import com.mintanable.notethepad.core.network.sync.CollaborationService
import com.mintanable.notethepad.database.db.repository.CollaborationRepository
import com.mintanable.notethepad.database.db.repository.CollaborationRepositoryImpl
import com.mintanable.notethepad.database.db.repository.NavigationDrawerItemRepository
import com.mintanable.notethepad.database.db.repository.NavigationDrawerItemRepositoryImpl
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.database.db.repository.NoteRepositoryImpl
import com.mintanable.notethepad.database.db.util.AudioMetadataProvider
import com.mintanable.notethepad.database.helper.DetailedNoteMapper
import com.mintanable.notethepad.database.preference.repository.SharedPreferencesRepository
import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import androidx.work.WorkManager
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
    fun provideNoteDatabase(manager: DatabaseManager): NoteDatabase {
        return manager.database
    }

    @Provides
    fun provideNoteDao(db: NoteDatabase): NoteDao {
        return db.noteDao()
    }

    @Provides
    fun provideTagDao(db: NoteDatabase): TagDao = db.tagDao()

    @Provides
    fun provideCollaboratorDao(db: NoteDatabase): CollaboratorDao = db.collaboratorDao()

    @Provides
    @Singleton
    fun provideNoteRepository(
        databaseManager: DatabaseManager,
        supabaseSyncService: SupabaseSyncService,
        userPreferencesRepository: UserPreferencesRepository,
        authRepository: AuthRepository,
        workManager: WorkManager,
        collaborationRepository: CollaborationRepository
    ): NoteRepository {
        return NoteRepositoryImpl(
            databaseManager,
            supabaseSyncService,
            userPreferencesRepository,
            authRepository,
            workManager,
            collaborationRepository
        )
    }

    @Provides
    @Singleton
    fun provideDetailedNoteMapper(audioMetadataProvider: AudioMetadataProvider, dispatchers: DispatcherProvider, dbManager: DatabaseManager): DetailedNoteMapper {
        return DetailedNoteMapper(audioMetadataProvider, dispatchers, dbManager.database.noteDao())
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

    @Provides
    @Singleton
    fun provideNavigationDrawerRepository(): NavigationDrawerItemRepository {
        return NavigationDrawerItemRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideCollaborationRepository(
        collaborationService: CollaborationService,
        collaboratorDao: CollaboratorDao,
        authRepository: AuthRepository,
        dbManager: DatabaseManager
    ): CollaborationRepository {
        return CollaborationRepositoryImpl(collaborationService, collaboratorDao, authRepository, dbManager)
    }
}