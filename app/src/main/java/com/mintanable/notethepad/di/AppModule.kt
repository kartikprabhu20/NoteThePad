package com.mintanable.notethepad.di

import com.mintanable.notethepad.AndroidAppVersionProvider
import com.mintanable.notethepad.core.common.AppVersionProvider
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.feature_navigationdrawer.data.repository.NavigationDrawerItemRepositoryImpl
import com.mintanable.notethepad.feature_navigationdrawer.domain.repository.NavigationDrawerItemRepository
import com.mintanable.notethepad.feature_navigationdrawer.domain.usecase.GetNavigationDrawerItems
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
        noteRepository: NoteRepository
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
    fun bindVersionProvider(): AppVersionProvider{
        return AndroidAppVersionProvider()
    }
}
