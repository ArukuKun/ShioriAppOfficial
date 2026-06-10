package com.example.shioriapp.data.repository

import android.util.Log
import com.example.shioriapp.domain.model.Chat
import com.example.shioriapp.domain.model.Message
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose

class ChatRepository(private val firestore: FirebaseFirestore) {

    private val TAG = "ChatRepository"

    suspend fun createPrivateChat(userId1: String, userId2: String): String {
        return try {
            val existingChat = firestore.collection("chats")
                .whereArrayContains("participants", userId1)
                .get()
                .await()
                .documents
                .firstOrNull { doc ->
                    val participants = doc.get("participants") as? List<*>
                    participants?.contains(userId2) == true
                }

            if (existingChat != null) return existingChat.id

            val chatId = firestore.collection("chats").document().id
            val chat = Chat(
                chatId = chatId,
                participants = listOf(userId1, userId2),
                lastMessage = "Nuevo chat creado",
                lastMessageTime = Date(),
                unreadCount = mapOf(userId1 to 0, userId2 to 0)
            )
            firestore.collection("chats").document(chatId).set(chat).await()
            chatId
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear chat privado", e)
            ""
        }
    }

    fun getUserChats(userId: String): Flow<List<Chat>> = callbackFlow {
        val subscription = firestore.collection("chats")
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando chats: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val chats = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Chat::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deserializando chat ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                // Ordenación en memoria para evitar crash por falta de índices
                val sortedChats = chats.sortedByDescending { it.lastMessageTime }
                trySend(sortedChats)
            }
        awaitClose { subscription.remove() }
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val subscription = firestore.collection("messages")
            .whereEqualTo("chatId", chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando mensajes: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Message::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deserializando mensaje ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                // Ordenación en memoria
                val sortedMessages = messages.sortedBy { it.timestamp }
                trySend(sortedMessages)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun sendMessage(chatId: String, senderId: String, text: String, imageUrl: String? = null): Message? {
        return try {
            val messageId = UUID.randomUUID().toString()
            val message = Message(
                messageId = messageId,
                chatId = chatId,
                senderId = senderId,
                text = text,
                imageUrl = imageUrl,
                timestamp = Date(),
                readBy = listOf(senderId)
            )
            
            firestore.collection("messages").document(messageId).set(message).await()

            val chatRef = firestore.collection("chats").document(chatId)
            val chatDoc = chatRef.get().await()
            val chat = chatDoc.toObject(Chat::class.java)
            
            if (chat != null) {
                val otherParticipant = chat.participants.firstOrNull { it != senderId }
                if (otherParticipant != null) {
                    val newUnreadCount = chat.unreadCount.toMutableMap()
                    newUnreadCount[otherParticipant] = (newUnreadCount[otherParticipant] ?: 0) + 1
                    chatRef.update(
                        mapOf(
                            "lastMessage" to text,
                            "lastMessageTime" to Date(),
                            "unreadCount" to newUnreadCount
                        )
                    ).await()
                }
            }
            message
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando mensaje", e)
            null
        }
    }

    suspend fun markMessagesAsRead(chatId: String, userId: String) {
        try {
            val chatRef = firestore.collection("chats").document(chatId)
            val chat = chatRef.get().await().toObject(Chat::class.java) ?: return
            
            val newUnread = chat.unreadCount.toMutableMap()
            newUnread[userId] = 0
            chatRef.update("unreadCount", newUnread).await()

            val messagesRef = firestore.collection("messages")
                .whereEqualTo("chatId", chatId)
                .get()
                .await()

            messagesRef.documents.forEach { doc ->
                val readBy = doc.get("readBy") as? List<*>
                if (readBy?.contains(userId) != true) {
                    doc.reference.update("readBy", FieldValue.arrayUnion(userId))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marcando mensajes como leídos", e)
        }
    }
}
