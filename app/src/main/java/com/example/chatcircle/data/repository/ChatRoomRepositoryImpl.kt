package com.example.chatcircle.data.repository

import com.example.chatcircle.domain.model.ChatRoom
import com.example.chatcircle.domain.repository.ChatRoomRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.UUID

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

            val roomId = UUID.randomUUID().toString()

            val room = ChatRoom(
                id = roomId,
                name = name,
                memberIds = memberIds
            )

            android.util.Log.d(
                "CHAT_ROOM",
                "Writing room to Firestore: $roomId"
            )

            withTimeout(15_000) {
                roomsCollection
                    .document(roomId)
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

    override suspend fun joinRoom(roomName: String): Result<ChatRoom> {
        return try {

            val currentUserId = com.google.firebase.auth.FirebaseAuth
                .getInstance()
                .currentUser
                ?.uid
                ?: return Result.failure(Exception("Not signed in"))

            val snapshot = roomsCollection
                .whereEqualTo("name", roomName)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                return Result.failure(Exception("Room not found"))
            }

            val roomDocument = snapshot.documents.first()

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
}