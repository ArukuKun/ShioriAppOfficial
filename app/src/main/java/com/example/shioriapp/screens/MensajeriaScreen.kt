package com.example.shioriapp.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.shioriapp.domain.model.Chat
import com.example.shioriapp.domain.model.UserProfile
import com.example.shioriapp.viewmodel.ChatListViewModel

import coil.compose.AsyncImage

@Composable
fun MensajeriaScreen(
    userId: String?,
    navController: NavController
) {
    if (userId.isNullOrEmpty()) {
        MensajeriaUnauthenticatedScreen()
    } else {
        MensajeriaAuthenticatedScreen(userId = userId, navController = navController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MensajeriaAuthenticatedScreen(
    userId: String,
    navController: NavController
) {
    val viewModel: ChatListViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatListViewModel(userId) as T
            }
        }
    )

    val chats by viewModel.chats.collectAsState()
    val friendRequests by viewModel.friendRequests.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val friends by viewModel.friends.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showIdInputDialog by remember { mutableStateOf(false) }
    var manualIdInput by remember { mutableStateOf("") }

    if (showIdInputDialog) {
        AlertDialog(
            onDismissRequest = { showIdInputDialog = false },
            title = { Text("Añadir amigo por ID") },
            text = {
                OutlinedTextField(
                    value = manualIdInput,
                    onValueChange = { manualIdInput = it },
                    label = { Text("ID de Usuario") },
                    placeholder = { Text("Pega el ID aquí...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (manualIdInput.isNotBlank()) {
                        navController.navigate("external_profile/$manualIdInput")
                        showIdInputDialog = false
                        manualIdInput = ""
                    }
                }) { Text("Buscar") }
            },
            dismissButton = {
                TextButton(onClick = { showIdInputDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                // 🔥 Empujamos el contenido hacia abajo para que la barra superior no lo tape
                .statusBarsPadding()
                .padding(top = 64.dp)
        ) {
            AnimatedVisibility(
                visible = showSearch,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchUsers(it)
                    },
                    placeholder = { Text("Buscar amigos...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; viewModel.searchUsers("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (showSearch && searchQuery.length >= 2) {
                    item {
                        Text(
                            text = "Resultados de búsqueda",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    if (searchResults.isEmpty()) {
                        item {
                            Text(
                                text = "No se encontraron usuarios",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(searchResults) { user ->
                            UserSearchItem(
                                user = user,
                                onAddFriendClick = { viewModel.sendFriendRequest(user.userId) }
                            )
                        }
                    }
                }
                else {
                    if (friendRequests.isNotEmpty()) {
                        item {
                            Text(
                                text = "Nuevas Solicitudes (${friendRequests.size})",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(friendRequests) { reqUser ->
                            FriendRequestItem(
                                user = reqUser,
                                onAcceptClick = { viewModel.acceptFriendRequest(reqUser.userId) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    if (friends.isNotEmpty()) {
                        item {
                            Text(
                                text = "Mis Amigos",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(friends) { friend ->
                            FriendItem(
                                user = friend,
                                onChatClick = {
                                    val existingChatId = viewModel.getChatIdWithFriend(friend.userId)
                                    if (existingChatId != null) {
                                        navController.navigate("chat_room/$existingChatId")
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    item {
                        Text(
                            text = "Mensajes",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (chats.isEmpty() && friends.isEmpty()) {
                        item { EmptyChatState() }
                    } else {
                        items(chats) { chat ->
                            ChatItem(chat = chat, onClick = {
                                navController.navigate("chat_room/${chat.chatId}")
                            })
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(150.dp)) }
            }
        }

        // 🔥 Botón Flotante con padding ajustado
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 140.dp),
            horizontalAlignment = Alignment.End
        ) {
            SmallFloatingActionButton(
                onClick = { showIdInputDialog = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Default.Link, contentDescription = "Añadir por ID")
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            FloatingActionButton(
                onClick = {
                    showSearch = !showSearch
                    if (!showSearch) {
                        searchQuery = ""
                        viewModel.searchUsers("")
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = if (showSearch) Icons.Default.Close else Icons.Default.PersonAdd,
                    contentDescription = if (showSearch) "Cerrar búsqueda" else "Añadir amigo"
                )
            }
        }
    }
}

// --- COMPONENTES VISUALES ---

@Composable
fun UserSearchItem(user: UserProfile, onAddFriendClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.photoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            error = androidx.compose.ui.res.painterResource(id = com.example.shioriapp.R.drawable.ic_shiori_black) // Ajusta el placeholder si es necesario
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = user.displayName ?: "Usuario desconocido",
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onAddFriendClick) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Añadir amigo", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun FriendRequestItem(user: UserProfile, onAcceptClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.photoUrl,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.displayName ?: "Usuario", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = "Quiere ser tu amigo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(
            onClick = onAcceptClick,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("Aceptar")
        }
    }
}

@Composable
fun FriendItem(user: UserProfile, onChatClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChatClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.photoUrl,
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))
        Text(text = user.displayName ?: "Amigo", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Chatear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ChatItem(chat: Chat, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = chat.otherUserPhotoUrl,
            contentDescription = null,
            modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = chat.otherUserName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                text = chat.lastMessage ?: "Toca para abrir la conversación",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptyChatState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = "Sin chats",
            modifier = Modifier.size(60.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Aún no tienes mensajes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "Busca amigos tocando el botón de abajo para empezar a chatear.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MensajeriaUnauthenticatedScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 64.dp)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Requiere Login",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Función bloqueada", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Necesitas iniciar sesión o crear una cuenta para poder enviar mensajes a otros usuarios.",
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}