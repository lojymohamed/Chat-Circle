package com.example.chatcircle.domain.repository

import com.example.chatcircle.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUser(uid: String): Result<User>

    /**
     * Every other user in the system, for starting a one-to-one chat.
     *
     * [excludeUid] is normally the signed-in user, who should not appear in
     * their own people list. Online users sort first, then alphabetically.
     */
    fun observeAllUsers(excludeUid: String): Flow<List<User>>
    fun observeOnlineStatus(uid: String): Flow<Boolean>
    suspend fun updatePresence(uid: String, isOnline: Boolean)
    suspend fun updateProfile(uid: String, displayName: String, photoUrl: android.net.Uri?): Result<Unit>
}