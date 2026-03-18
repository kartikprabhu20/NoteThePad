package com.mintanable.notethepad.di

import com.google.firebase.auth.FirebaseAuth
import com.mintanable.notethepad.AndroidAppVersionProvider
import com.mintanable.notethepad.core.common.AppVersionProvider
import com.mintanable.notethepad.feature_firebase.data.repository.AuthRepositoryImpl
import com.mintanable.notethepad.feature_firebase.domain.repository.AuthRepository
import com.mintanable.notethepad.feature_navigationdrawer.data.repository.NavigationDrawerItemRepositoryImpl
import com.mintanable.notethepad.feature_navigationdrawer.domain.repository.NavigationDrawerItemRepository
import com.mintanable.notethepad.feature_navigationdrawer.domain.usecase.GetNavigationDrawerItems
import com.mintanable.notethepad.database.repository.NoteRepository
import dagger.Binds
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
    fun provideGetNavigationDrawerItemsUseCase(
        repository: NavigationDrawerItemRepository,
        noteRepository: com.mintanable.notethepad.database.repository.NoteRepository
    ): GetNavigationDrawerItems {
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
    fun bindVersionProvider(): AppVersionProvider{
        return AndroidAppVersionProvider()
    }
}
