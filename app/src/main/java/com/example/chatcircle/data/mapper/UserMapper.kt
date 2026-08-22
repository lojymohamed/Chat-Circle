package com.example.chatcircle.data.mapper

import com.example.chatcircle.domain.model.User
import com.google.firebase.auth.FirebaseUser

fun FirebaseUser.toDomainUser(): User {
    return User(
        uid = uid,
        displayName = displayName ?: "",
        email = email ?: "",
        photoUrl = photoUrl?.toString(),
        isOnline = true
    )
}