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

) {
    val viewModel: ChatListViewModel = viewModel(key = userId) { ChatListViewModel(userId) }
    val chats by viewModel.chats.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val friendRequests by viewModel.friendRequests.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var searchQuery by remember { mutableStateOf("") }


                }
            }
            
            Box(modifier = Modifier.fillMaxSize()) {
                when (tabIndex) {
                    0 -> ChatList(chats, onChatClick = { chat ->
                        navController.navigate("chat/${chat.chatId}/${chat.otherUserName}")
                    })
                    1 -> FriendList(friends, onMessageClick = { friend ->
                        val existingChatId = viewModel.getChatIdWithFriend(friend.userId)
                        if (existingChatId != null) {
                            navController.navigate("chat/$existingChatId/${friend.displayName}")
                        } else {
                            navController.navigate("new_chat/${friend.userId}/${friend.displayName}")
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

                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun FriendList(friends: List<UserProfile>, onMessageClick: (UserProfile) -> Unit) {

            }
        }
    }
}

@Composable
fun FriendRequestsList(requests: List<UserProfile>, onAccept: (UserProfile) -> Unit) {

                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun AddFriendItem(user: UserProfile, onAdd: () -> Unit) {

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
