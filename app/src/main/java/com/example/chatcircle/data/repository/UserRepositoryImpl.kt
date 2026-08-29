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

private const val TAG = "CC_UserRepo"

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore, // Inject Firestore
    private val auth: FirebaseAuth,
    private val storageRepository: StorageRepository
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override suspend fun getUser(uid: String): Result<User> {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            if (snapshot.exists()) {
                val user = User( // Create a User object from the snapshot
                    uid = snapshot.getString("uid") ?: uid,
                    displayName = snapshot.getString("displayName") ?: "",
                    email = snapshot.getString("email") ?: "",
                    photoUrl = snapshot.getString("photoUrl"),
                    isOnline = snapshot.getString("status") == "online" // returns a boolean
                )
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Streams the whole users collection, minus [excludeUid].
     *
     * Firestore cannot express "not equal to" cheaply alongside an ordering, so
     * the signed-in user is filtered out client side and the sort is done here
     * too - the collection is small enough that this is far simpler than
     * maintaining a composite index for it.
     */
    override fun observeAllUsers(excludeUid: String): Flow<List<User>> = callbackFlow {
        android.util.Log.d(TAG, "observeAllUsers() called: excludeUid=$excludeUid")

        val listener = usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e(TAG, "observeAllUsers() failed", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            val users = snapshot?.documents.orEmpty().mapNotNull { document ->
                val uid = document.getString("uid") ?: document.id
                if (uid == excludeUid) return@mapNotNull null

                User(
                    uid = uid,
                    displayName = document.getString("displayName").orEmpty(),
                    email = document.getString("email").orEmpty(),
                    photoUrl = document.getString("photoUrl"),
                    isOnline = document.getString("status") == "online"
                )
            }.sortedWith(
                compareByDescending<User> { it.isOnline }
                    .thenBy { it.displayName.lowercase() }
            )

            android.util.Log.d(TAG, "observeAllUsers() success: count=${users.size}")
            trySend(users)
        }

        awaitClose { listener.remove() }
    }

    override fun observeOnlineStatus(uid: String): Flow<Boolean> = callbackFlow { //callback flow acts as a bridge firestore uses callback while kotlin uses flow
        val listener = usersCollection.document(uid) //flow is a stream of booleans
            .addSnapshotListener { snapshot, error -> // listen to user document and notify when it changes
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(false) //send through the flow
                    return@addSnapshotListener
                }
                val status = snapshot.getString("status") ?: "offline"
                trySend(status == "online")
            }
        awaitClose { listener.remove() } //memory leaks
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
