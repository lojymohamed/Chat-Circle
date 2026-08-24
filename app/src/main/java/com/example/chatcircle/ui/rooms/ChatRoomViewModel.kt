package com.example.chatcircle.ui.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatcircle.domain.model.ChatRoom
import com.example.chatcircle.domain.repository.ChatRoomRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChatRoomUiState {
    object Idle : ChatRoomUiState()
    object Loading : ChatRoomUiState()
    data class Success(val message: String, val room: ChatRoom) : ChatRoomUiState()
    data class Error(val message: String) : ChatRoomUiState()
}

// NEW: one-time events, separate from uiState
sealed class ChatRoomNavigationEvent {
    data class NavigateToChatRoom(val roomId: String, val roomName: String) : ChatRoomNavigationEvent()
}

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val chatRoomRepository: ChatRoomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatRoomUiState>(ChatRoomUiState.Idle)
    val uiState: StateFlow<ChatRoomUiState> = _uiState

    // NEW
    private val _navigationEvent = MutableSharedFlow<ChatRoomNavigationEvent>()
    val navigationEvent: SharedFlow<ChatRoomNavigationEvent> = _navigationEvent

    fun createRoom(roomName: String) {
        if (roomName.isBlank()) {
            _uiState.value = ChatRoomUiState.Error("Room name can't be empty")
            return
        }

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            _uiState.value = ChatRoomUiState.Error("You must be signed in")
            return
        }

        viewModelScope.launch {
            _uiState.value = ChatRoomUiState.Loading
            val result = chatRoomRepository.createRoom(
                name = roomName,
                memberIds = listOf(currentUserId)
            )
            result.fold(
                onSuccess = { room ->
                    _uiState.value = ChatRoomUiState.Success(
                        message = "Joined room '${room.name}'!",
                        room = room
                    )
                    // NEW: fire navigation
                    _navigationEvent.emit(
                        ChatRoomNavigationEvent.NavigateToChatRoom(room.id, room.name)
                    )
                },
                onFailure = {
                    _uiState.value = ChatRoomUiState.Error(it.message ?: "Failed to join room")
                }
            )
        }
    }

    fun joinRoom(roomName: String) {
        if (roomName.isBlank()) {
            _uiState.value = ChatRoomUiState.Error("Room name can't be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = ChatRoomUiState.Loading
            val result = chatRoomRepository.joinRoom(roomName)
            result.fold(
                onSuccess = { room ->
                    _uiState.value = ChatRoomUiState.Success(
                        message = "Room '${room.name}' created!",
                        room = room
                    )
                    // NEW: fire navigation
                    _navigationEvent.emit(
                        ChatRoomNavigationEvent.NavigateToChatRoom(room.id, room.name)
                    )
                },
                onFailure = {
                    _uiState.value = ChatRoomUiState.Error(it.message ?: "Failed to create room")
                }
            )
        }
    }
}