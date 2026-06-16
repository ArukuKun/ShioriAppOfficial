package com.example.shioriapp.auth

import android.net.Uri
import com.example.shioriapp.data.repository.ChatRepository
import com.example.shioriapp.domain.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UserManager(private val firestore: FirebaseFirestore) {
    suspend fun createOrUpdateUserProfile(userId: String, profile: UserProfile) {
        firestore.collection("users").document(userId).set(profile).await()
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        val doc = firestore.collection("users").document(userId).get().await()
        return doc.toObject(UserProfile::class.java)
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val subscription = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(UserProfile::class.java))
            }
        awaitClose { subscription.remove() }
    }

    suspend fun searchUsersByDisplayName(query: String): List<UserProfile> {
        val snapshot = firestore.collection("users")
            .whereGreaterThanOrEqualTo("displayName", query)
            .whereLessThanOrEqualTo("displayName", query + '\uf8ff')
            .limit(20)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
    }

    suspend fun sendFriendRequest(fromUserId: String, toUserId: String) {
        val fromUser = getUserProfile(fromUserId) ?: return
        val toUser = getUserProfile(toUserId) ?: return
        // Actualizar ambos
        val updatedFrom = fromUser.copy(friendRequestsSent = fromUser.friendRequestsSent + toUserId)
        val updatedTo = toUser.copy(friendRequestsReceived = toUser.friendRequestsReceived + fromUserId)
        createOrUpdateUserProfile(fromUserId, updatedFrom)
        createOrUpdateUserProfile(toUserId, updatedTo)
    }

    suspend fun acceptFriendRequest(currentUserId: String, requesterId: String) {
        val current = getUserProfile(currentUserId) ?: return
        val requester = getUserProfile(requesterId) ?: return
        val newCurrent = current.copy(
            friendRequestsReceived = current.friendRequestsReceived - requesterId,
            friends = current.friends + requesterId
        )
        val newRequester = requester.copy(
            friendRequestsSent = requester.friendRequestsSent - currentUserId,
            friends = requester.friends + currentUserId
        )
        createOrUpdateUserProfile(currentUserId, newCurrent)
        createOrUpdateUserProfile(requesterId, newRequester)
        // Crear chat si no existe
        ChatRepository(FirebaseFirestore.getInstance()).createPrivateChat(currentUserId, requesterId)
    }

    suspend fun uploadProfileImage(userId: String, imageUri: Uri): String? {
        return try {
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("profile_images/${userId}_${UUID.randomUUID()}.jpg")
            imageRef.putFile(imageUri).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            android.util.Log.e("UserManager", "Error uploading profile image", e)
            null
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        newName: String,
        newPhotoUrl: String?,
        bio: String? = null,
        birthday: String? = null,
        mangaInterests: List<String> = emptyList()
    ) {
        try {
            val userProfile = getUserProfile(userId) ?: return
            val updatedProfile = userProfile.copy(
                displayName = newName,
                photoUrl = newPhotoUrl ?: userProfile.photoUrl,
                bio = bio ?: userProfile.bio,
                birthday = birthday ?: userProfile.birthday,
                mangaInterests = if (mangaInterests.isNotEmpty()) mangaInterests else userProfile.mangaInterests
            )
            createOrUpdateUserProfile(userId, updatedProfile)
            
            // Si estuviéramos usando un sistema donde el nombre y foto se guardan en el chat/mensaje directamente, 
            // habría que actualizarlos aquí. Afortunadamente en tu sistema, la UI obtiene la foto y nombre leyendo 
            // la lista de "amigos" de Firestore, así que al actualizar el UserProfile ya se reflejará en la UI de todos.
        } catch (e: Exception) {
            android.util.Log.e("UserManager", "Error updating profile", e)
        }
    }
}