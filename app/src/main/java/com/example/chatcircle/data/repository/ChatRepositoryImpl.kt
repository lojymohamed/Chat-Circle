package com.example.chatcircle.data.repository

import com.example.chatcircle.data.local.LocalDbProvider
import com.example.chatcircle.data.mapper.toDomain
import com.example.chatcircle.data.mapper.toEntity
import com.example.chatcircle.domain.model.Message
import com.example.chatcircle.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val localDbProvider: LocalDbProvider,
    private val auth: FirebaseAuth
) : ChatRepository {

    // Background scope for syncing Firestore snapshots into Room.
    // Not tied to any UI lifecycle — lives as long as the repository (singleton).
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun messagesCollection(roomId: String) =
        firestore.collection("chatRooms")
            .document(roomId)
            .collection("messages")

    private fun remoteMessagesFlow(roomId: String): Flow<List<Message>> = callbackFlow {
        val listener = messagesCollection(roomId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Room is the single source of truth for the UI. Firestore's snapshotListener
     * runs in the background and upserts into Room; the UI only ever collects Room.
     */
    override fun observeMessages(roomId: String): Flow<List<Message>> {
        val uid = auth.currentUser?.uid ?: return emptyFlow()
        val dao = localDbProvider.open(uid).messageDao()

        syncScope.launch {
            remoteMessagesFlow(roomId).collect { remoteMessages ->
                dao.upsertAll(remoteMessages.map { it.toEntity(roomId) })
            }
        }

        return dao.observeMessages(roomId).map { entities -> entities.map { it.toDomain() } }
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

            // Write-through: sender sees their own message instantly without
            // waiting for the snapshot listener round-trip.
            auth.currentUser?.uid?.let { uid ->
                localDbProvider.open(uid).messageDao().upsert(message.toEntity(roomId))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendImageMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        imageUrl: String
    ): Result<Unit> {
        return try {
            val message = Message(
                id = UUID.randomUUID().toString(),
                senderId = senderId,
                senderName = senderName,
                text = null,
                imageUrl = imageUrl,
                timestamp = System.currentTimeMillis()
            )
            messagesCollection(roomId)
                .document(message.id)
                .set(message)
                .await()

            auth.currentUser?.uid?.let { uid ->
                localDbProvider.open(uid).messageDao().upsert(message.toEntity(roomId))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deliberately NOT routed through Room — Room only holds data for rooms
     * currently being observed. Unread counts must work for rooms the user
     * hasn't opened yet, so this stays a direct Firestore aggregate query.
     */
    override suspend fun getUnreadCount(
        roomId: String,
        sinceTimestamp: Long
    ): Result<Int> {
        return try {
            val snapshot = messagesCollection(roomId)
                .whereGreaterThan("timestamp", sinceTimestamp)
                .count()
                .get(AggregateSource.SERVER)
                .await()

            Result.success(snapshot.count.toInt())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}