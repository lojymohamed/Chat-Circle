package com.example.chatcircle.data.repository

import com.example.chatcircle.domain.model.ChatRoom
import com.example.chatcircle.domain.repository.ChatRoomRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class ChatRoomRepositoryImpl(
    private val firestore: FirebaseFirestore
) : ChatRoomRepository {

    private val roomsCollection = firestore.collection("chatRooms")

    override suspend fun createRoom(
        name: String,
        memberIds: List<String>
    ): Result<ChatRoom> {
        return try {
            android.util.Log.d("CHAT_ROOM", "Starting room creation")

            val roomCode = generateAvailableRoomCode()

            val room = ChatRoom(
                id = roomCode,
                name = name,
                memberIds = memberIds
            )

            android.util.Log.d(
                "CHAT_ROOM",
                "Writing room to Firestore: $roomCode"
            )

            withTimeout(15_000) {
                roomsCollection
                    .document(roomCode)
                    .set(room)
                    .await()
            }

            android.util.Log.d("CHAT_ROOM", "Room successfully created!")

            Result.success(room)

        } catch (e: Exception) {
            android.util.Log.e("CHAT_ROOM", "Failed to create room", e)
            Result.failure(e)
        }
    }

    override suspend fun joinRoom(roomCode: String): Result<ChatRoom> {
        return try {

            val currentUserId = com.google.firebase.auth.FirebaseAuth
                .getInstance()
                .currentUser
                ?.uid
                ?: return Result.failure(Exception("Not signed in"))

            val roomDocument = roomsCollection
                .document(roomCode.trim().uppercase())
                .get()
                .await()

            if (!roomDocument.exists()) {
                return Result.failure(Exception("Room not found"))
            }

            val room = roomDocument.toObject(ChatRoom::class.java)
                ?: return Result.failure(Exception("Invalid room data"))

            roomDocument.reference.update(
                "memberIds",
                com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId)
            ).await()

            Result.success(room)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeUserRooms(userId: String): Flow<List<ChatRoom>> = callbackFlow {
        val listener = roomsCollection
            .whereArrayContains("memberIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val rooms = snapshot?.toObjects(ChatRoom::class.java) ?: emptyList()
                trySend(rooms)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun markRoomAsRead(
        roomId: String,
        userId: String
    ): Result<Unit> {
        return try {
            roomsCollection
                .document(roomId)
                .update("lastReadTimestamps.$userId", System.currentTimeMillis())
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun generateAvailableRoomCode(): String {
        repeat(MAX_CODE_GENERATION_ATTEMPTS) {
            val code = buildString(ROOM_CODE_LENGTH) {
                repeat(ROOM_CODE_LENGTH) {
                    append(ROOM_CODE_CHARACTERS.random())
                }
            }
            if (!roomsCollection.document(code).get().await().exists()) {
                return code
            }
        }
        throw IllegalStateException("Could not generate a unique room code. Please try again.")
    }

    private companion object {
        const val ROOM_CODE_LENGTH = 6
        const val MAX_CODE_GENERATION_ATTEMPTS = 5
        const val ROOM_CODE_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
