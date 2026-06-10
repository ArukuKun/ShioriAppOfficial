package com.example.shioriapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.shioriapp.domain.model.Chat
import com.example.shioriapp.domain.model.UserProfile
import com.example.shioriapp.viewmodel.ChatListViewModel
import androidx.navigation.NavController
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    userId: String,
    navController: NavController,
    onOpenChat: (String, String) -> Unit // chatId, friendName
) {
    val viewModel: ChatListViewModel = viewModel(key = userId) { ChatListViewModel(userId) }
    val chats by viewModel.chats.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val friendRequests by viewModel.friendRequests.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddFriend by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mensajes") },
                actions = {
                    IconButton(onClick = { showAddFriend = true }) {
                        Icon(Icons.Default.PersonAdd, "Añadir amigo")
                    }
                }
            )
        },
        floatingActionButton = {
            if (showAddFriend) {
                FloatingActionButton(onClick = { showAddFriend = false }) {
                    Icon(Icons.Default.Close, null)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showAddFriend) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        viewModel.searchUsers(query)
                    },
                    placeholder = { Text("Buscar por nombre...") },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(searchResults) { user ->
                        AddFriendItem(user, onAdd = { viewModel.sendFriendRequest(user.userId) })
                    }
                }
            } else {
                // Pestañas: Chats / Amigos / Solicitudes
                var tabIndex by remember { mutableStateOf(0) }
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Chats") })
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Amigos") })
                    Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Solicitudes (${friendRequests.size})") })
                }
                when (tabIndex) {
                    0 -> ChatList(chats, onChatClick = { chat ->
                        val friendId = chat.participants.first { it != userId }
                        val friendName = friends.find { it.userId == friendId }?.displayName ?: "Amigo"
                        onOpenChat(chat.chatId, friendName)
                        navController.navigate("chat/${chat.chatId}/$friendName")
                    })
                    1 -> FriendList(friends, onMessageClick = { friend ->
                        val existingChatId = viewModel.getChatIdWithFriend(friend.userId)
                        if (existingChatId != null) {
                            onOpenChat(existingChatId, friend.displayName)
                            navController.navigate("chat/$existingChatId/${friend.displayName}")
                        } else {
                            // Crear nuevo chat
                            navController.navigate("new_chat/${friend.userId}/${friend.displayName}")
                        }
                    })
                    2 -> FriendRequestsList(friendRequests, onAccept = { viewModel.acceptFriendRequest(it.userId) })
                }
            }
        }
    }
}

@Composable
fun ChatList(chats: List<Chat>, onChatClick: (Chat) -> Unit) {
    LazyColumn {
        items(chats) { chat ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onChatClick(chat) }.padding(8.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(chat.chatId.take(8), fontWeight = FontWeight.Bold)
                        Text(chat.lastMessage ?: "Sin mensajes", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FriendList(friends: List<UserProfile>, onMessageClick: (UserProfile) -> Unit) {
    LazyColumn {
        items(friends) { friend ->
            Row(Modifier.fillMaxWidth().clickable { onMessageClick(friend) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = friend.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(16.dp))
                Text(friend.displayName, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Message, null)
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun FriendRequestsList(requests: List<UserProfile>, onAccept: (UserProfile) -> Unit) {
    LazyColumn {
        items(requests) { requester ->
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = requester.photoUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(requester.displayName, fontWeight = FontWeight.Bold)
                    Text("Solicitud de amistad", fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onAccept(requester) }) { Text("Aceptar") }
            }
        }
    }
}

@Composable
fun AddFriendItem(user: UserProfile, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = user.photoUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(user.displayName, fontWeight = FontWeight.Bold)
            Text(user.email ?: "", fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onAdd) { Icon(Icons.Default.PersonAdd, null) }
    }
}