package com.example.chatcircle.data.repository

import android.net.Uri
import com.example.chatcircle.domain.model.User
import com.example.chatcircle.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storageRepository: StorageRepository
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override suspend fun getUser(uid: String): Result<User> {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            if (snapshot.exists()) {
                val user = User(
                    uid = snapshot.getString("uid") ?: uid,
                    displayName = snapshot.getString("displayName") ?: "",
                    email = snapshot.getString("email") ?: "",
                    photoUrl = snapshot.getString("photoUrl"),
                    isOnline = snapshot.getString("status") == "online"
                )
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeOnlineStatus(uid: String): Flow<Boolean> = callbackFlow {
        val listener = usersCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(false)
                    return@addSnapshotListener
                }
                val status = snapshot.getString("status") ?: "offline"
                trySend(status == "online")
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updatePresence(uid: String, isOnline: Boolean) {
        try {
            usersCollection.document(uid).update(
                mapOf(
                    "status" to if (isOnline) "online" else "offline",
                    "lastSeen" to System.currentTimeMillis()
                )
            ).await()
        } catch (_: Exception) {
            // Silently fail — presence is best-effort
        }
    }

    override suspend fun updateProfile(uid: String, displayName: String, photoUrl: Uri?): Result<Unit> {
        return try {
            var finalPhotoUrl: String? = null

            // 1. Upload photo if provided
            if (photoUrl != null) {
                val uploadResult = storageRepository.uploadImage("profile_images", photoUrl)
                if (uploadResult.isSuccess) {
                    finalPhotoUrl = uploadResult.getOrNull()
                } else {
                    return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Image upload failed"))
                }
            }

            // 2. Update FirebaseAuth Profile
            val user = auth.currentUser
            if (user != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                
                if (finalPhotoUrl != null) {
                    profileUpdates.setPhotoUri(Uri.parse(finalPhotoUrl))
                }
                
                user.updateProfile(profileUpdates.build()).await()
            }

            // 3. Update Firestore Document
            val updates = mutableMapOf<String, Any>(
                "displayName" to displayName
            )
            if (finalPhotoUrl != null) {
                updates["photoUrl"] = finalPhotoUrl
            }
            usersCollection.document(uid).update(updates).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertUser(uid: String, displayName: String, email: String, photoUrl: String?) {
        try {
            val data = mutableMapOf<String, Any>(
                "uid" to uid,
                "displayName" to displayName,
                "email" to email,
                "status" to "online",
                "lastSeen" to System.currentTimeMillis()
            )
            if (photoUrl != null) {
                data["photoUrl"] = photoUrl
            }
            usersCollection.document(uid).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (_: Exception) {
            // Best-effort upsert
        }
    }
}
