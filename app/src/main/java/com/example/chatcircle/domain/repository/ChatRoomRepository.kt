package com.example.chatcircle.domain.repository

import com.example.chatcircle.domain.model.ChatRoom
import kotlinx.coroutines.flow.Flow

interface ChatRoomRepository {

    suspend fun createRoom(
        name: String,
        memberIds: List<String>
    ): Result<ChatRoom>

    suspend fun joinRoom(
        roomName: String
    ): Result<ChatRoom>

    fun observeUserRooms(
        userId: String
    ): Flow<List<ChatRoom>>
}