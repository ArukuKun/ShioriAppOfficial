package com.example.shioriapp.domain.model

import java.util.Date

data class Message(
    var messageId: String = "",
    var chatId: String = "",
    var senderId: String = "",
    var text: String = "",
    var imageUrl: String? = null,
    var timestamp: Date = Date(),
    var readBy: List<String> = emptyList()
)
