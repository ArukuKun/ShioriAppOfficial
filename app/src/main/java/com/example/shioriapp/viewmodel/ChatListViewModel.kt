package com.example.shioriapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shioriapp.data.repository.ChatRepository
import com.example.shioriapp.domain.model.Chat
import com.example.shioriapp.domain.model.UserProfile
import com.example.shioriapp.auth.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatListViewModel(private val userId: String) : ViewModel() {
    private val chatRepository = ChatRepository(FirebaseFirestore.getInstance())
    private val userManager = UserManager(FirebaseFirestore.getInstance())

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _friends = MutableStateFlow<List<UserProfile>>(emptyList())
    val friends: StateFlow<List<UserProfile>> = _friends.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<UserProfile>>(emptyList())
    val friendRequests: StateFlow<List<UserProfile>> = _friendRequests.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UserProfile>>(emptyList())
    val searchResults: StateFlow<List<UserProfile>> = _searchResults.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.getUserChats(userId).collect { chats ->
                _chats.value = chats
            }
        }
        viewModelScope.launch { loadFriendsAndRequests() } // ✅ envolver en launch
    }

    private suspend fun loadFriendsAndRequests() {
        val profile = userManager.getUserProfile(userId) ?: return
        val friendsList = profile.friends.mapNotNull { userManager.getUserProfile(it) }
        val requestsList = profile.friendRequestsReceived.mapNotNull { userManager.getUserProfile(it) }
        _friends.value = friendsList
        _friendRequests.value = requestsList
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            if (query.length < 2) {
                _searchResults.value = emptyList()
                return@launch
            }
            val results = userManager.searchUsersByDisplayName(query)
            _searchResults.value = results.filter { it.userId != userId }
        }
    }

    fun sendFriendRequest(toUserId: String) {
        viewModelScope.launch {
            userManager.sendFriendRequest(userId, toUserId)
            loadFriendsAndRequests()
        }
    }

    fun acceptFriendRequest(fromUserId: String) {
        viewModelScope.launch {
            userManager.acceptFriendRequest(userId, fromUserId)
            loadFriendsAndRequests()
        }
    }

    fun getChatIdWithFriend(friendId: String): String? {
        return _chats.value.find { chat -> chat.participants.containsAll(listOf(userId, friendId)) }?.chatId
    }
}