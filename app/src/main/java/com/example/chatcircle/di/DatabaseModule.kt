package com.example.chatcircle.di

import android.content.Context
import com.example.chatcircle.data.local.LocalDbProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLocalDbProvider(@ApplicationContext context: Context): LocalDbProvider =
        LocalDbProvider(context)
}