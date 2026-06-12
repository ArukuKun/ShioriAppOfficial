package com.example.shioriapp.data.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseStorageService {
    private val storage = FirebaseStorage.getInstance().reference

    suspend fun uploadImage(chatId: String, localUri: Uri): String? {
        val fileName = "chats/$chatId/${UUID.randomUUID()}.jpg"
        val fileRef = storage.child(fileName)
        return try {
            fileRef.putFile(localUri).await()
            val downloadUrl = fileRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            null
        }
    }
}