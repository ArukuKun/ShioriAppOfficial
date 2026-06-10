package com.example.shioriapp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shioriapp.data.remote.FirebaseStorageService
import com.example.shioriapp.data.repository.ChatRepository
import com.example.shioriapp.domain.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatId: String,
    private val currentUserId: String
) : ViewModel() {
    private val chatRepository = ChatRepository(FirebaseFirestore.getInstance())
    private val storageService = FirebaseStorageService()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { msgs ->
                _messages.value = msgs
                markAsRead()
            }
        }
    }

    private fun markAsRead() {
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(chatId, currentUserId)
        }
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isSending.value = true
            chatRepository.sendMessage(chatId, currentUserId, text)
            _isSending.value = false
        }
    }

    fun sendImage(uri: Uri) {
        viewModelScope.launch {
            _isSending.value = true
            val imageUrl = storageService.uploadImage(chatId, uri)
            if (imageUrl != null) {
                chatRepository.sendMessage(chatId, currentUserId, "", imageUrl)
            }
            _isSending.value = false
        }
    }
}