package com.example.chatcircle.data.repository

import com.example.chatcircle.data.remote.FirebaseAuthDataSource
import com.example.chatcircle.domain.model.User
import com.example.chatcircle.domain.repository.AuthRepository
import javax.inject.Inject
import com.example.chatcircle.data.mapper.toDomainUser

class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: FirebaseAuthDataSource
) : AuthRepository {

    override suspend fun signUp(
        email: String,
        password: String
    ): Result<User> {
        return try {
            val firebaseUser = authDataSource.signUp(email, password)

            if (firebaseUser != null) {
                Result.success(firebaseUser.toDomainUser())
            } else {
                Result.failure(Exception("User creation failed"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(
        email: String,
        password: String
    ): Result<User> {
        return try {
            val firebaseUser = authDataSource.signIn(email, password)

            if (firebaseUser != null) {
                Result.success(firebaseUser.toDomainUser())
            } else {
                Result.failure(Exception("Sign in failed"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(
        idToken: String
    ): Result<User> {
        return try {
            val firebaseUser = authDataSource.signInWithGoogle(idToken)

            if (firebaseUser != null) {
                Result.success(firebaseUser.toDomainUser())
            } else {
                Result.failure(Exception("Google sign in failed"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun currentUser(): User? {
        return authDataSource.currentUser()?.toDomainUser()
    }

    override fun signOut() {
        authDataSource.signOut()
    }
}