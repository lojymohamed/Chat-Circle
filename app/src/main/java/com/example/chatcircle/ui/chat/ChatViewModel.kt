package com.example.chatcircle.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatcircle.domain.model.Message
import com.example.chatcircle.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChatUiState {
    object Loading : ChatUiState()
    data class Success(val messages: List<Message>) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val roomId: String = checkNotNull(savedStateHandle["roomId"])
    val roomName: String = checkNotNull(savedStateHandle["roomName"])

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState

    init {
        chatRepository.observeMessages(roomId)
            .let { flow ->
                viewModelScope.launch {
                    flow.collect { messages ->
                        _uiState.value = ChatUiState.Success(messages)
                    }
                }
            }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            _uiState.value = ChatUiState.Error("You must be signed in to send messages")
            return
        }
        val senderName = user.displayName ?: user.email ?: "Unknown"

        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                roomId = roomId,
                senderId = user.uid,
                senderName = senderName,
                text = text
            )
            if (result.isFailure) {
                _uiState.value = ChatUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to send message"
                )
            }
        }
    }
}