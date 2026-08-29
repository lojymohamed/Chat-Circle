package com.example.chatcircle.ui.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatcircle.domain.model.ChatRoom
import com.example.chatcircle.domain.model.User
import com.example.chatcircle.domain.repository.AuthRepository
import com.example.chatcircle.domain.repository.ChatRepository
import com.example.chatcircle.domain.repository.ChatRoomRepository
import com.example.chatcircle.domain.repository.UserRepository
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

sealed class ChatRoomNavigationEvent {
    data class NavigateToChatRoom(val roomId: String, val roomName: String) : ChatRoomNavigationEvent()
    data class ShowCreatedRoomCode(val room: ChatRoom) : ChatRoomNavigationEvent()
}

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatRoomUiState>(ChatRoomUiState.Idle)
    val uiState: StateFlow<ChatRoomUiState> = _uiState

    private val _navigationEvent = MutableSharedFlow<ChatRoomNavigationEvent>()
    val navigationEvent: SharedFlow<ChatRoomNavigationEvent> = _navigationEvent

    private val _userRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val userRooms: StateFlow<List<ChatRoom>> = _userRooms

    private val _unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unreadCounts: StateFlow<Map<String, Int>> = _unreadCounts

    /**
     * Everyone else signed up, for starting a one-to-one chat.
     *
     * Empty until the users collection reports back, so the home screen shows
     * its empty state rather than a spinner - a people strip that is briefly
     * blank reads better than one that flashes a loader on every open.
     */
    private val _people = MutableStateFlow<List<User>>(emptyList())
    val people: StateFlow<List<User>> = _people

    /**
     * The signed-in user, for the identity row on Home.
     *
     * Read once rather than observed: name and photo only change from the
     * profile screen, which is a separate destination, so the value is always
     * re-read when Home is returned to.
     */
    fun currentUser(): User? = authRepository.currentUser()

    init {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId != null) {
            viewModelScope.launch {
                userRepository.observeAllUsers(currentUserId).collect { users ->
                    _people.value = users
                }
            }
        }

        if (currentUserId != null) {
            viewModelScope.launch {
                chatRoomRepository.observeUserRooms(currentUserId)
                    .collect { rooms ->
                        _userRooms.value = rooms
                        refreshUnreadCounts(rooms, currentUserId)
                    }
            }
        }
    }

    private fun refreshUnreadCounts(rooms: List<ChatRoom>, currentUserId: String) {
        viewModelScope.launch {
            val counts = mutableMapOf<String, Int>()
            for (room in rooms) {
                val since = room.lastReadTimestamps[currentUserId] ?: 0L
                val result = chatRepository.getUnreadCount(room.id, since)
                counts[room.id] = result.getOrDefault(0)
            }
            _unreadCounts.value = counts
        }
    }

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
                        message = "Created room '${room.name}'!",
                        room = room
                    )
                    _navigationEvent.emit(ChatRoomNavigationEvent.ShowCreatedRoomCode(room))
                },
                onFailure = {
                    _uiState.value = ChatRoomUiState.Error(it.message ?: "Failed to create room")
                }
            )
        }
    }

    fun joinRoom(roomCode: String) {
        if (roomCode.isBlank()) {
            _uiState.value = ChatRoomUiState.Error("Room code can't be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = ChatRoomUiState.Loading
            val result = chatRoomRepository.joinRoom(roomCode)
            result.fold(
                onSuccess = { room ->
                    _uiState.value = ChatRoomUiState.Success(
                        message = "Joined room '${room.name}'!",
                        room = room
                    )
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
}
