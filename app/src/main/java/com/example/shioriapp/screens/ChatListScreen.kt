package com.example.shioriapp.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.shioriapp.domain.model.Chat
import com.example.shioriapp.domain.model.UserProfile
import com.example.shioriapp.viewmodel.ChatListViewModel

@Composable
fun ChatListScreen(
    userId: String,
    navController: NavHostController,
    showAddFriend: Boolean = false,
    onShowAddFriendChange: (Boolean) -> Unit = {}
) {
    val viewModel: ChatListViewModel = viewModel(key = userId) { ChatListViewModel(userId) }
    val chats by viewModel.chats.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val friendRequests by viewModel.friendRequests.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Espacio para la TopAppBar estandarizado
        Spacer(modifier = Modifier.height(96.dp))

        if (showAddFriend) {
            TextField(
                value = searchQuery,
                onValueChange = { query ->
                    searchQuery = query
                    viewModel.searchUsers(query)
                },
                placeholder = { Text("Nombre de usuario...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
                items(searchResults) { user ->
                    AddFriendItem(user, onAdd = { viewModel.sendFriendRequest(user.userId) })
                }
            }
        } else {
            var tabIndex by remember { mutableStateOf(0) }
            
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        height = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                val tabs = listOf("Chats", "Amigos", "Solicitudes")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = { 
                            val label = if (index == 2 && friendRequests.isNotEmpty()) "$title (${friendRequests.size})" else title
                            Text(
                                label, 
                                fontWeight = if (tabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    )
                }
            }
            
            Box(modifier = Modifier.fillMaxSize()) {
                when (tabIndex) {
                    0 -> ChatList(chats, onChatClick = { chat ->
                        navController.navigate("chat_room/${chat.chatId}")
                    })
                    1 -> FriendList(friends, onMessageClick = { friend ->
                        val existingChatId = viewModel.getChatIdWithFriend(friend.userId)
                        if (existingChatId != null) {
                            navController.navigate("chat_room/$existingChatId")
                        } else {
                            viewModel.createChatWithFriend(friend.userId) { newChatId ->
                                navController.navigate("chat_room/$newChatId")
                            }
                        }
                    })
                    2 -> FriendRequestsList(friendRequests, onAccept = { requester ->
                        viewModel.acceptFriendRequest(requester.userId)
                    })
                }
            }
        }
    }
}

@Composable
fun ChatList(chats: List<Chat>, onChatClick: (Chat) -> Unit) {
    if (chats.isEmpty()) {
        EmptyStateView(Icons.Default.ChatBubbleOutline, "No hay conversaciones aún")
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
            items(chats) { chat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChatClick(chat) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = chat.otherUserPhotoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(chat.otherUserName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            chat.lastMessage ?: "Sin mensajes", 
                            maxLines = 1, 
                            fontSize = 13.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FriendList(friends: List<UserProfile>, onMessageClick: (UserProfile) -> Unit) {
    if (friends.isEmpty()) {
        EmptyStateView(Icons.Default.PeopleOutline, "Aún no tienes amigos agregados")
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
            items(friends) { friend ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onMessageClick(friend) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = friend.photoUrl,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(friend.displayName, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.Message, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun FriendRequestsList(requests: List<UserProfile>, onAccept: (UserProfile) -> Unit) {
    if (requests.isEmpty()) {
        EmptyStateView(Icons.Default.PersonSearch, "No hay solicitudes pendientes")
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
            items(requests) { requester ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = requester.photoUrl,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(requester.displayName, fontWeight = FontWeight.Bold)
                        Text("Quiere ser tu amigo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { onAccept(requester) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Text("Aceptar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddFriendItem(user: UserProfile, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.photoUrl,
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.displayName, fontWeight = FontWeight.Bold)
            Text(user.email ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onAdd) { 
            Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.primary) 
        }
    }
}

@Composable
fun EmptyStateView(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}
