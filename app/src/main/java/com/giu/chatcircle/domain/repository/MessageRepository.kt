package com.giu.chatcircle.domain.repository

import com.giu.chatcircle.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun observeMessages(roomId: String): Flow<List<Message>>
    suspend fun sendMessage(roomId: String, message: Message): Result<Unit>
}