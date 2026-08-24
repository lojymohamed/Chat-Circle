package com.example.chatcircle.di

import com.example.chatcircle.data.repository.AuthRepositoryImpl
import com.example.chatcircle.data.repository.ChatRepositoryImpl
import com.example.chatcircle.data.repository.ChatRoomRepositoryImpl
import com.example.chatcircle.domain.repository.AuthRepository
import com.example.chatcircle.domain.repository.ChatRepository
import com.example.chatcircle.domain.repository.ChatRoomRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
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

    @Provides
    @Singleton
    fun provideChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository {
        return chatRepositoryImpl
    }

}