package com.example.chatcircle.di

import com.example.chatcircle.domain.usecase.auth.SignInUseCase
import com.example.chatcircle.domain.usecase.auth.SignInWithGoogleUseCase
import com.example.chatcircle.domain.usecase.auth.SignUpUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideSignInUseCase(
        authRepository: com.example.chatcircle.domain.repository.AuthRepository
    ): SignInUseCase {
        return SignInUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideSignInWithGoogleUseCase(
        authRepository: com.example.chatcircle.domain.repository.AuthRepository
    ): SignInWithGoogleUseCase {
        return SignInWithGoogleUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideSignUpUseCase(
        authRepository: com.example.chatcircle.domain.repository.AuthRepository
    ): SignUpUseCase {
        return SignUpUseCase(authRepository)
    }
}
