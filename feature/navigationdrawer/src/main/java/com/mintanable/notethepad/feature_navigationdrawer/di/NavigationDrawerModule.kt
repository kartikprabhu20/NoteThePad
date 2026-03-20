package com.mintanable.notethepad.feature_navigationdrawer.di

import com.mintanable.notethepad.database.db.repository.NavigationDrawerItemRepository
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.feature_navigationdrawer.domain.usecase.GetNavigationDrawerItems
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class NavigationDrawerModule {

    @Provides
    @Singleton
    fun provideGetNavigationDrawerItemsUseCase(
        repository: NavigationDrawerItemRepository,
        noteRepository: NoteRepository
    ): GetNavigationDrawerItems {
        return GetNavigationDrawerItems(repository, noteRepository)
    }
}