package com.example.chatcircle.domain.repository

import com.example.chatcircle.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUser(uid: String): Result<User>
    fun observeOnlineStatus(uid: String): Flow<Boolean>
    suspend fun updatePresence(uid: String, isOnline: Boolean)
    suspend fun updateProfile(uid: String, displayName: String, photoUrl: android.net.Uri?): Result<Unit>
    suspend fun updateFcmToken(uid: String, fcmToken: String): Result<Unit>
}