package com.example.shioriapp.domain.model

import java.util.Date

data class Chat(
    var chatId: String = "",
    var participants: List<String> = emptyList(),
    var lastMessage: String? = null,
    var lastMessageTime: Date = Date(),
    var unreadCount: Map<String, Int> = emptyMap(),
    var type: String = "private"
)
