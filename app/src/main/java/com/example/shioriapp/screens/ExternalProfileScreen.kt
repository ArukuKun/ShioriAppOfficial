package com.example.shioriapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.shioriapp.auth.UserManager
import com.example.shioriapp.domain.model.UserProfile
import com.example.shioriapp.viewmodel.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalProfileScreen(
    userId: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val userManager = remember { UserManager(FirebaseFirestore.getInstance()) }
    var targetUser by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var requestSent by remember { mutableStateOf(false) }

    // Obtenemos el ID del usuario actual de algún lado si es posible
    // Para simplificar, asumiremos que el sistema de amistad maneja el "from" correctamente
    // Pero necesitamos el ID del que está logueado actualmente.
    // Usaremos un LocalAuthViewModel o similar si estuviera disponible, 
    // pero por ahora buscaremos al usuario objetivo.

    LaunchedEffect(userId) {
        isLoading = true
        targetUser = userManager.getUserProfile(userId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil de Usuario", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (targetUser == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Usuario no encontrado", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("El ID proporcionado no corresponde a ningún usuario.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val user = targetUser!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(id = com.example.shioriapp.R.drawable.ic_shiori_black)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = user.displayName ?: "Usuario Shiori",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "ID: ${user.userId}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (!user.bio.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = user.bio!!,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (requestSent) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("¡Solicitud enviada!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                // Aquí hay un problema: no tenemos el currentUserId fácilmente.
                                // En una app real lo sacaríamos del AuthViewModel.
                                // Por ahora, mostraremos un aviso o intentaremos obtenerlo si es posible.
                                scope.launch {
                                    val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                    if (currentUid != null) {
                                        userManager.sendFriendRequest(currentUid, user.userId)
                                        requestSent = true
                                    } else {
                                        // TODO: Mostrar diálogo de login
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Añadir a mis amigos", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
