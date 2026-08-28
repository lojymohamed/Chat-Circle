package com.example.chatcircle.data.mapper

import com.example.chatcircle.data.local.entity.MessageEntity
import com.example.chatcircle.domain.model.Message

fun Message.toEntity(roomId: String): MessageEntity = MessageEntity(
    id = id,
    roomId = roomId,
    senderId = senderId,
    senderName = senderName,
    text = text,
    imageUrl = imageUrl,
    timestamp = timestamp
)

fun MessageEntity.toDomain(): Message = Message(
    id = id,
    senderId = senderId,
    senderName = senderName,
    text = text,
    imageUrl = imageUrl,
    timestamp = timestamp
)