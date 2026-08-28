package com.example.chatcircle.data.mapper

import com.example.chatcircle.data.local.entity.ChatRoomEntity
import com.example.chatcircle.domain.model.ChatRoom

fun ChatRoom.toEntity(): ChatRoomEntity = ChatRoomEntity(
    id = id,
    name = name,
    memberIds = memberIds,
    lastMessage = lastMessage,
    lastMessageTimestamp = lastMessageTimestamp,
    lastReadTimestamps = lastReadTimestamps
)

fun ChatRoomEntity.toDomain(): ChatRoom = ChatRoom(
    id = id,
    name = name,
    memberIds = memberIds,
    lastMessage = lastMessage,
    lastMessageTimestamp = lastMessageTimestamp,
    lastReadTimestamps = lastReadTimestamps
)