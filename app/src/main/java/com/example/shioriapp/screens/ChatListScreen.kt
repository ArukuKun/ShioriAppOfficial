package com.example.shioriapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.shioriapp.domain.model.Chat
import com.example.shioriapp.domain.model.UserProfile
import com.example.shioriapp.viewmodel.ChatListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    userId: String,
    navController: NavController
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
                    IconButton(onClick = { showAddFriend = !showAddFriend }) {
                        Icon(if (showAddFriend) Icons.Default.Close else Icons.Default.PersonAdd, "Añadir amigo")
                    }
                }
            )
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
                if (searchResults.isEmpty() && searchQuery.length >= 2) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No se encontraron usuarios")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(searchResults) { user ->
                            AddFriendItem(user, onAdd = { viewModel.sendFriendRequest(user.userId) })
                        }
                    }
                }
            } else {
                var tabIndex by remember { mutableStateOf(0) }
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Chats") })
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Amigos") })
                    Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Solicitudes (${friendRequests.size})") })
                }
                when (tabIndex) {
                    0 -> {
                        if (chats.isEmpty()) {
                            EmptyState(Icons.Default.Chat, "No tienes conversaciones activas")
                        } else {
                            ChatList(chats, userId, friends, onChatClick = { chatId, friendName ->
                                navController.navigate("chat/$chatId/$friendName")
                            })
                        }
                    }
                    1 -> {
                        if (friends.isEmpty()) {
                            EmptyState(Icons.Default.People, "Aún no tienes amigos agregados")
                        } else {
                            FriendList(friends, onMessageClick = { friend ->
                                val existingChatId = viewModel.getChatIdWithFriend(friend.userId)
                                if (existingChatId != null) {
                                    navController.navigate("chat/$existingChatId/${friend.displayName}")
                                } else {
                                    navController.navigate("new_chat/${friend.userId}/${friend.displayName}")
                                }
                            })
                        }
                    }
                    2 -> {
                        if (friendRequests.isEmpty()) {
                            EmptyState(Icons.Default.Mail, "No tienes solicitudes pendientes")
                        } else {
                            FriendRequestsList(friendRequests, onAccept = { viewModel.acceptFriendRequest(it.userId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Text(message, color = Color.Gray)
    }
}

@Composable
fun ChatList(chats: List<Chat>, currentUserId: String, friends: List<UserProfile>, onChatClick: (String, String) -> Unit) {
    LazyColumn {
        items(chats) { chat ->
            val friendId = chat.participants.find { it != currentUserId } ?: ""
            val friendName = friends.find { it.userId == friendId }?.displayName ?: "Usuario"
            
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onChatClick(chat.chatId, friendName) }.padding(8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(friendName, fontWeight = FontWeight.Bold)
                        Text(chat.lastMessage ?: "Inicia una conversación", fontSize = 12.sp, maxLines = 1)
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
