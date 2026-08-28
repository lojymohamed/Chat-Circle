package com.example.chatcircle.di

import com.example.chatcircle.data.local.LocalDbProvider
import com.example.chatcircle.data.repository.AuthRepositoryImpl
import com.example.chatcircle.data.repository.ChatRepositoryImpl
import com.example.chatcircle.data.repository.ChatRoomRepositoryImpl
import com.example.chatcircle.data.repository.StorageRepository
import com.example.chatcircle.data.repository.UserRepositoryImpl
import com.example.chatcircle.domain.repository.AuthRepository
import com.example.chatcircle.domain.repository.ChatRepository
import com.example.chatcircle.domain.repository.ChatRoomRepository
import com.example.chatcircle.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
        firestore: FirebaseFirestore,
        localDbProvider: LocalDbProvider
    ): ChatRoomRepository {
        return ChatRoomRepositoryImpl(firestore, localDbProvider)
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

    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        storageRepository: StorageRepository
    ): UserRepository {
        return UserRepositoryImpl(firestore, auth, storageRepository)
    }

    @Provides
    @Singleton
    fun provideUserRepositoryImpl(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        storageRepository: StorageRepository
    ): UserRepositoryImpl {
        return UserRepositoryImpl(firestore, auth, storageRepository)
    }

    @Provides
    @Singleton
    fun provideStorageRepository(
        storage: FirebaseStorage
    ): StorageRepository {
        return StorageRepository(storage)
    }
}