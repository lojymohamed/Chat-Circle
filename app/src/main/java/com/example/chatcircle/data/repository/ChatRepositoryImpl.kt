package com.example.chatcircle.data.repository

import com.example.chatcircle.domain.model.Message
import com.example.chatcircle.domain.repository.ChatRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    private fun messagesCollection(roomId: String) =
        firestore.collection("chatRooms")
            .document(roomId)
            .collection("messages")

    override fun observeMessages(roomId: String): Flow<List<Message>> = callbackFlow {

        val listener = messagesCollection(roomId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages =
                    snapshot?.toObjects(Message::class.java) ?: emptyList()

                trySend(messages)
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun sendMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        text: String
    ): Result<Unit> {
        return try {
            val message = Message(
                id = UUID.randomUUID().toString(),
                senderId = senderId,
                senderName = senderName,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            messagesCollection(roomId)
                .document(message.id)
                .set(message)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}