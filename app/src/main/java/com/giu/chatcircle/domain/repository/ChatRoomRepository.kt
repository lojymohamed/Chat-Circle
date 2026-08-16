package com.giu.chatcircle.domain.repository

import com.giu.chatcircle.domain.model.ChatRoom
import kotlinx.coroutines.flow.Flow

interface ChatRoomRepository {
    suspend fun createRoom(name: String, memberIds: List<String>): Result<ChatRoom>
    suspend fun joinRoom(roomId: String): Result<Unit>
    fun observeUserRooms(userId: String): Flow<List<ChatRoom>>
}