package com.example.chatcircle.data.di

import com.example.chatcircle.data.repository.AuthRepositoryImpl
import com.example.chatcircle.data.repository.ChatRoomRepositoryImpl
import com.example.chatcircle.domain.repository.AuthRepository
import com.example.chatcircle.domain.repository.ChatRoomRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideChatRoomRepository(
        firestore: FirebaseFirestore
    ): ChatRoomRepository {
        return ChatRoomRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository {
        return authRepositoryImpl
    }
}