package com.example.chatcircle.data.repository

import com.example.chatcircle.domain.model.ChatRoom
import com.example.chatcircle.domain.repository.ChatRoomRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRoomRepositoryImpl(
    private val firestore: FirebaseFirestore
) : ChatRoomRepository {

    private val roomsCollection = firestore.collection("chatRooms")

    override suspend fun createRoom(name: String, memberIds: List<String>): Result<ChatRoom> {
        return try {
            val roomId = UUID.randomUUID().toString()
            val room = ChatRoom(
                id = roomId,
                name = name,
                memberIds = memberIds
            )
            roomsCollection.document(roomId).set(room).await()
            Result.success(room)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinRoom(roomId: String): Result<Unit> {
        return try {
            val roomRef = roomsCollection.document(roomId)
            val snapshot = roomRef.get().await()

            if (!snapshot.exists()) {
                return Result.failure(Exception("Room not found"))
            }

            val currentUserId = com.google.firebase.auth.FirebaseAuth
                .getInstance().currentUser?.uid
                ?: return Result.failure(Exception("Not signed in"))

            roomRef.update(
                "memberIds",
                com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId)
            ).await()

            Result.success(Unit)
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
}