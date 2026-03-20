package com.mintanable.notethepad.di

import com.mintanable.notethepad.AndroidAppVersionProvider
import com.mintanable.notethepad.core.common.AppVersionProvider
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
    fun bindVersionProvider(): AppVersionProvider{
        return AndroidAppVersionProvider()
    }
}
