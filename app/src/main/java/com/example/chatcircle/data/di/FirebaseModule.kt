package com.example.chatcircle.data.di

import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module //tells dagger this is a module
@InstallIn(SingletonComponent::class) //tells dagger this is a singleton
object FirebaseModule {

    @Provides
    @Singleton //only create one instance of firebase auth
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}