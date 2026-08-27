package com.example.chatcircle.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage
) {

    suspend fun uploadImage(roomId: String, imageUri: Uri): Result<String> { //uri is the location of the image on the user's device
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val ref = storage.reference //where the image will be stored
                .child("chat_images")
                .child(roomId)
                .child(fileName)

            ref.putFile(imageUri).await() //upload the image to the storage
            val downloadUrl = ref.downloadUrl.await().toString() //get the download url of the image
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
