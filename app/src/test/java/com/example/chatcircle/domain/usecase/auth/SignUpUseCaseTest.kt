package com.example.chatcircle.domain.usecase.auth

import com.example.chatcircle.domain.model.User
import com.example.chatcircle.domain.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SignUpUseCaseTest {

    @Mock
    private lateinit var authRepository: AuthRepository

    private lateinit var signUpUseCase: SignUpUseCase

    private val testUser = User(
        uid = "test-id",
        email = "test@example.com",
        displayName = "Test User"
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        signUpUseCase = SignUpUseCase(authRepository)
    }

    @Test
    fun `invoke with blank email returns failure`() = runTest {
        val result = signUpUseCase(email = "   ", password = "password123")

        assertTrue(result.isFailure)
        assertEquals("Email cannot be empty", result.exceptionOrNull()?.message)
        verify(authRepository, never()).signUp(any(), any())
    }

    @Test
    fun `invoke with blank password returns failure`() = runTest {
        val result = signUpUseCase(email = "test@example.com", password = "   ")

        assertTrue(result.isFailure)
        assertEquals("Password cannot be empty", result.exceptionOrNull()?.message)
        verify(authRepository, never()).signUp(any(), any())
    }

    @Test
    fun `invoke with short password returns failure`() = runTest {
        val result = signUpUseCase(email = "test@example.com", password = "12345")

        assertTrue(result.isFailure)
        assertEquals("Password must be at least 6 characters", result.exceptionOrNull()?.message)
        verify(authRepository, never()).signUp(any(), any())
    }

    @Test
    fun `invoke with valid credentials calls repository and returns success`() = runTest {
        whenever(authRepository.signUp("test@example.com", "password123"))
            .thenReturn(Result.success(testUser))

        val result = signUpUseCase(email = "test@example.com", password = "password123")

        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
        verify(authRepository).signUp("test@example.com", "password123")
    }

    @Test
    fun `invoke with repository error returns failure`() = runTest {
        whenever(authRepository.signUp(any(), any()))
            .thenReturn(Result.failure(Exception("Email already exists")))

        val result = signUpUseCase(email = "test@example.com", password = "password123")

        assertTrue(result.isFailure)
        assertEquals("Email already exists", result.exceptionOrNull()?.message)
    }
}
