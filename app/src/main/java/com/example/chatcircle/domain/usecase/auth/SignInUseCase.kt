package com.example.chatcircle.domain.usecase.auth

import com.example.chatcircle.domain.model.User
import com.example.chatcircle.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke(
        email: String,
        password: String
    ): Result<User> {

        if (email.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Email cannot be empty")
            )
        }

        if (password.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Password cannot be empty")
            )
        }

        return authRepository.signIn(
            email = email,
            password = password
        )
    }
}