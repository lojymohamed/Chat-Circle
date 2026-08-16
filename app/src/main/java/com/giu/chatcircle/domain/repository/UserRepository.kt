package com.giu.chatcircle.domain.repository

import com.giu.chatcircle.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUser(uid: String): Result<User>
    fun observeOnlineStatus(uid: String): Flow<Boolean>
    suspend fun updatePresence(uid: String, isOnline: Boolean)
}