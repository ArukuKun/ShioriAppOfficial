package com.example.shioriapp.auth

import com.example.shioriapp.data.repository.ChatRepository
import com.example.shioriapp.domain.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserManager(private val firestore: FirebaseFirestore) {
    suspend fun createOrUpdateUserProfile(userId: String, profile: UserProfile) {
        firestore.collection("users").document(userId).set(profile).await()
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        val doc = firestore.collection("users").document(userId).get().await()
        return doc.toObject(UserProfile::class.java)
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
}