package com.mintanable.notethepad.feature_firebase.di

import com.google.firebase.auth.FirebaseAuth
import com.mintanable.notethepad.feature_firebase.data.repository.AuthRepositoryImpl
import com.mintanable.notethepad.feature_firebase.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(auth: FirebaseAuth): AuthRepository {
        return AuthRepositoryImpl(auth)
    }
}