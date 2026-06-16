package com.example.shioriapp.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.shioriapp.domain.model.Message
import com.example.shioriapp.viewmodel.ChatViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    chatId: String,
    currentUserId: String,
    navController: NavController
) {
    // Instanciamos tu ChatViewModel
    val viewModel: ChatViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(chatId, currentUserId) as T
            }
        }
    )

    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val typingUsers by viewModel.typingUsers.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Detectar cuando el usuario está escribiendo
    LaunchedEffect(messageText) {
        if (messageText.isNotBlank()) {
            viewModel.setTyping(true)
            delay(3000) // Mantener estado por 3s o hasta que cambie
            viewModel.setTyping(false)
        } else {
            viewModel.setTyping(false)
        }
    }

    // Selector de imágenes de Android
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendImage(it) }
    }

    // Auto-scroll hacia abajo cuando llega un nuevo mensaje
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            val otherTyping = typingUsers.filter { it.key != currentUserId && it.value }.isNotEmpty()
            
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Chat", fontWeight = FontWeight.Bold) 
                        if (otherTyping) {
                            Text("Escribiendo...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                text = messageText,
                onTextChange = { messageText = it },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendTextMessage(messageText)
                        messageText = ""
                    }
                },
                onImageClick = { imagePickerLauncher.launch("image/*") },
                isSending = isSending
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                // Asumo que tu modelo Message tiene estas propiedades. Ajusta los nombres si es necesario.
                val isMine = message.senderId == currentUserId
                MessageBubble(message = message, isMine = isMine)
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isMine: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isMine) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp)
        ) {
            Column {
                // Si el mensaje tiene una imagen
                if (!message.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Imagen enviada",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Si el mensaje tiene texto
                if (message.text.isNotEmpty()) {
                    Text(
                        text = message.text,
                        color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onImageClick: () -> Unit,
    isSending: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = onImageClick, enabled = !isSending) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Enviar imagen",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                placeholder = { Text("Escribe un mensaje...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                )
            )

            Box(
                modifier = Modifier
                    .padding(start = 4.dp, bottom = 4.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = text.isNotBlank() && !isSending) { onSendClick() },
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = if (text.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}