package com.example.chatcircle.ui.auth.login

import com.example.chatcircle.domain.model.User
import com.example.chatcircle.domain.usecase.auth.SignInUseCase
import com.example.chatcircle.domain.usecase.auth.SignInWithGoogleUseCase
import com.example.chatcircle.ui.auth.AuthUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @Mock
    private lateinit var signInUseCase: SignInUseCase

    @Mock
    private lateinit var signInWithGoogleUseCase: SignInWithGoogleUseCase

    private lateinit var loginViewModel: LoginViewModel

    private val testUser = User(
        uid = "test-id",
        email = "test@example.com",
        displayName = "Test User"
    )

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        loginViewModel = LoginViewModel(signInUseCase, signInWithGoogleUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login with valid credentials updates state to success`() = runTest {
        whenever(signInUseCase("test@example.com", "password123"))
            .thenReturn(Result.success(testUser))

        loginViewModel.login("test@example.com", "password123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = loginViewModel.uiState.value
        assertTrue(state is AuthUiState.Success)
        assertEquals(testUser, (state as AuthUiState.Success).user)
    }

    @Test
    fun `login with invalid credentials updates state to error`() = runTest {
        whenever(signInUseCase("test@example.com", "wrong"))
            .thenReturn(Result.failure(Exception("Invalid credentials")))

        loginViewModel.login("test@example.com", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = loginViewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals("Invalid credentials", (state as AuthUiState.Error).message)
    }

    @Test
    fun `login with Google token updates state to success`() = runTest {
        whenever(signInWithGoogleUseCase("google-id-token"))
            .thenReturn(Result.success(testUser))

        loginViewModel.loginWithGoogle("google-id-token")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = loginViewModel.uiState.value
        assertTrue(state is AuthUiState.Success)
        assertEquals(testUser, (state as AuthUiState.Success).user)
    }
}
