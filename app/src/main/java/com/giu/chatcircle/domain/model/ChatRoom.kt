package com.giu.chatcircle.domain.model

data class ChatRoom(
    val id: String = "",
    val name: String = "",
    val memberIds: List<String> = emptyList(),
    val lastMessage: String? = null,
    val lastMessageTimestamp: Long? = null
)