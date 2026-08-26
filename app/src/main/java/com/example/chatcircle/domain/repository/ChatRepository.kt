package com.example.chatcircle.domain.repository

import com.example.chatcircle.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    fun observeMessages(roomId: String): Flow<List<Message>>

    suspend fun sendMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        text: String
    ): Result<Unit>

    suspend fun sendImageMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        imageUrl: String
    ): Result<Unit>
}