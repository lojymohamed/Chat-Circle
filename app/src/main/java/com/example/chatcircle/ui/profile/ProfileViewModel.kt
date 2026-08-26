package com.example.chatcircle.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatcircle.domain.model.User
import com.example.chatcircle.domain.repository.AuthRepository
import com.example.chatcircle.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(val message: String) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val user = MutableStateFlow<User?>(null)

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        val currentUser = authRepository.currentUser()
        if (currentUser != null) {
            user.value = currentUser
            viewModelScope.launch {
                val result = userRepository.getUser(currentUser.uid)
                result.onSuccess { fetchedUser ->
                    user.value = fetchedUser
                }
            }
        }
    }

    fun updateProfile(name: String, imageUri: Uri?) {
        val currentUser = authRepository.currentUser()
        if (currentUser == null) {
            _uiState.value = ProfileUiState.Error("User is not signed in")
            return
        }

        if (name.isBlank()) {
            _uiState.value = ProfileUiState.Error("Display name cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val result = userRepository.updateProfile(
                uid = currentUser.uid,
                displayName = name,
                photoUrl = imageUri
            )
            result.onSuccess {
                _uiState.value = ProfileUiState.Success("Profile updated successfully")
                val updatedResult = userRepository.getUser(currentUser.uid)
                updatedResult.onSuccess { updatedUser ->
                    user.value = updatedUser
                }
            }.onFailure { exception ->
                _uiState.value = ProfileUiState.Error(
                    exception.message ?: "Failed to update profile"
                )
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
