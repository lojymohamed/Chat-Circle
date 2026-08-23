package com.example.chatcircle.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatcircle.domain.usecase.auth.SignUpUseCase
import com.example.chatcircle.ui.auth.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<AuthUiState>(AuthUiState.Idle)

    val uiState: StateFlow<AuthUiState> =
        _uiState.asStateFlow()

    fun register(
        email: String,
        password: String,
        confirmPassword: String
    ) {

        if (password != confirmPassword) {
            _uiState.value = AuthUiState.Error(
                "Passwords do not match"
            )
            return
        }

        viewModelScope.launch {

            _uiState.value = AuthUiState.Loading

            val result = signUpUseCase(
                email = email,
                password = password
            )

            result
                .onSuccess { user ->
                    _uiState.value =
                        AuthUiState.Success(user)
                }
                .onFailure { exception ->
                    _uiState.value =
                        AuthUiState.Error(
                            exception.message
                                ?: "Registration failed"
                        )
                }
        }
    }
}