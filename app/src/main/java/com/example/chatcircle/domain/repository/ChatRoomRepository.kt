package com.example.chatcircle.domain.repository

import com.example.chatcircle.domain.model.ChatRoom
import kotlinx.coroutines.flow.Flow

interface ChatRoomRepository {

    suspend fun createRoom(
        name: String,
        memberIds: List<String>
    ): Result<ChatRoom>

    suspend fun joinRoom(roomCode: String): Result<ChatRoom>

    fun observeUserRooms(
        userId: String
    ): Flow<List<ChatRoom>>

    suspend fun markRoomAsRead(
        roomId: String,
        userId: String,
        readAt: Long
    ): Result<Unit>
}
