package com.example.chatcircle.ui.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatcircle.data.repository.StorageRepository
import com.example.chatcircle.domain.model.Message
import com.example.chatcircle.domain.repository.ChatRepository
import com.example.chatcircle.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val roomId: String = checkNotNull(savedStateHandle["roomId"])
    val roomName: String = checkNotNull(savedStateHandle["roomName"])

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState

    private val _onlineCount = MutableStateFlow(0)
    val onlineCount: StateFlow<Int> = _onlineCount

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    private val presenceJobs = mutableMapOf<String, Job>()
    private val onlineUsers = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            chatRepository.observeMessages(roomId)
                .collect { messages ->
                    _uiState.value = ChatUiState.Success(messages)
                    
                    // Simple presence tracking based on users who have sent messages
                    val uniqueUserIds = messages.map { it.senderId }.distinct()
                    uniqueUserIds.forEach { uid ->
                        if (!presenceJobs.containsKey(uid)) {
                            val job = launch {
                                userRepository.observeOnlineStatus(uid).collect { isOnline ->
                                    if (isOnline) {
                                        onlineUsers.add(uid)
                                    } else {
                                        onlineUsers.remove(uid)
                                    }
                                    _onlineCount.value = onlineUsers.size
                                }
                            }
                            presenceJobs[uid] = job
                        }
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

    fun sendImageMessage(imageUri: Uri) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            _uiState.value = ChatUiState.Error("You must be signed in to send messages")
            return
        }
        val senderName = user.displayName ?: user.email ?: "Unknown"

        viewModelScope.launch {
            _isUploading.value = true
            val uploadResult = storageRepository.uploadImage(roomId, imageUri)
            uploadResult.fold(
                onSuccess = { downloadUrl ->
                    val sendResult = chatRepository.sendImageMessage(
                        roomId = roomId,
                        senderId = user.uid,
                        senderName = senderName,
                        imageUrl = downloadUrl
                    )
                    if (sendResult.isFailure) {
                        _uiState.value = ChatUiState.Error(
                            sendResult.exceptionOrNull()?.message ?: "Failed to send image message"
                        )
                    }
                },
                onFailure = {
                    _uiState.value = ChatUiState.Error(
                        it.message ?: "Failed to upload image"
                    )
                }
            )
            _isUploading.value = false
        }
    }
}