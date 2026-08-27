package com.example.chatcircle.domain.model

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val isOnline: Boolean = false,
    val fcmToken: String? = null
)