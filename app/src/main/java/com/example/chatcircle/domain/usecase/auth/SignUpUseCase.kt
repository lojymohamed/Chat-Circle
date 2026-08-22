package com.example.chatcircle.domain.usecase.auth

import com.example.chatcircle.domain.model.User
import com.example.chatcircle.domain.repository.AuthRepository
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke( // call usecase object as function

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

        if (password.length < 6) {
            return Result.failure(
                IllegalArgumentException(
                    "Password must be at least 6 characters"
                )
            )
        }

        return authRepository.signUp(
            email = email,
            password = password
        )
    }
}