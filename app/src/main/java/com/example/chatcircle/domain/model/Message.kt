package com.example.chatcircle.domain.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String? = null,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)