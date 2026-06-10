package com.example.shioriapp.domain.model

import java.util.Date

data class UserProfile(
    var userId: String = "",
    var email: String? = null,
    var displayName: String = "",
    var photoUrl: String? = null,
    var discordId: String? = null,
    var discordUsername: String? = null,
    var friends: List<String> = emptyList(),
    var friendRequestsReceived: List<String> = emptyList(),
    var friendRequestsSent: List<String> = emptyList(),
    var createdAt: java.util.Date = java.util.Date(),
    var lastSeen: java.util.Date = java.util.Date()
)
