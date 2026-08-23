package com.example.chatcircle.ui.auth

import com.example.chatcircle.domain.model.User

sealed class AuthUiState {

    data object Idle : AuthUiState()

    data object Loading : AuthUiState()

    data class Success(
        val user: User
    ) : AuthUiState()

    data class Error(
        val message: String
    ) : AuthUiState()
}