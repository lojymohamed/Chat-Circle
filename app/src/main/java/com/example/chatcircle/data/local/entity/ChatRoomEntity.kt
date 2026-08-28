package com.example.chatcircle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_rooms")
data class ChatRoomEntity(
    @PrimaryKey val id: String,
    val name: String,
    val memberIds: List<String>,
    val lastMessage: String?,
    val lastMessageTimestamp: Long?,
    val lastReadTimestamps: Map<String, Long>
)