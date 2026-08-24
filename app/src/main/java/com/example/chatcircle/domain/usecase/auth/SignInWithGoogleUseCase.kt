package com.example.chatcircle.domain.usecase.auth

import com.example.chatcircle.domain.model.User
import com.example.chatcircle.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke(
        idToken: String
    ): Result<User> {
        return authRepository.signInWithGoogle(idToken)
    }
}