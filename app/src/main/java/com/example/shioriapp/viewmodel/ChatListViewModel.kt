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
import kotlinx.coroutines.flow.collectLatest
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
        // Observar Perfil de Usuario para cambios en amigos y solicitudes
        viewModelScope.launch {
            userManager.observeUserProfile(userId).collectLatest { profile ->
                profile?.let {
                    val friendsList = it.friends.mapNotNull { fId -> userManager.getUserProfile(fId) }
                    val requestsList = it.friendRequestsReceived.mapNotNull { rId -> userManager.getUserProfile(rId) }
                    _friends.value = friendsList
                    _friendRequests.value = requestsList
                }
            }
        }

        // Observar Chats
        viewModelScope.launch {
            chatRepository.getUserChats(userId).collectLatest { chats ->
                val enrichedChats = chats.map { chat ->
                    val otherParticipantId = chat.participants.find { it != userId } ?: ""
                    val otherProfile = userManager.getUserProfile(otherParticipantId)
                    chat.copy(
                        otherUserName = otherProfile?.displayName ?: "Usuario Shiori",
                        otherUserPhotoUrl = otherProfile?.photoUrl
                    )
                }
                _chats.value = enrichedChats
            }
        }
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
            // Ya no es necesario llamar a loadFriendsAndRequests() porque observeUserProfile se encarga
        }
    }

    fun acceptFriendRequest(fromUserId: String) {
        viewModelScope.launch {
            userManager.acceptFriendRequest(userId, fromUserId)
            // Ya no es necesario llamar a loadFriendsAndRequests() porque observeUserProfile se encarga
        }
    }

    fun getChatIdWithFriend(friendId: String): String? {
        return _chats.value.find { chat -> chat.participants.containsAll(listOf(userId, friendId)) }?.chatId
    }

    fun createChatWithFriend(friendId: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val chatId = chatRepository.createPrivateChat(userId, friendId)
            onResult(chatId)
        }
    }
}