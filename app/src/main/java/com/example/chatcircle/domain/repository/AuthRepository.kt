package com.example.chatcircle.domain.repository

import com.example.chatcircle.domain.model.User

interface AuthRepository {
    suspend fun signUp(email: String, password: String): Result<User>
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    fun currentUser(): User?
    fun signOut()
}