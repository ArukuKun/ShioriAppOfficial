package com.example.shioriapp.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
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
import coil.compose.AsyncImage
import com.example.shioriapp.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val userProfile = (authState as? AuthViewModel.AuthState.Authenticated)?.profile

    // 1. Observar estado de carga y errores
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val isLoading = authState is AuthViewModel.AuthState.Loading

    if (userProfile == null && !isLoading) {
        // Fallback si no está autenticado
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Debes iniciar sesión para ver tu perfil")
        }
        return
    }

    var name by remember { mutableStateOf(userProfile?.displayName ?: "") }
    var bio by remember { mutableStateOf(userProfile?.bio ?: "") }
    var birthday by remember { mutableStateOf(userProfile?.birthday ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val availableGenres = listOf("Acción", "Romance", "Fantasía", "Sci-Fi", "Comedia", "Drama", "Horror", "Slice of Life", "Isekai")
    var selectedGenres by remember { mutableStateOf(userProfile?.mangaInterests?.toSet() ?: emptySet()) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    // 2. Lógica inteligente para esperar a que termine la subida a Firebase antes de volver
    var wasLoading by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
        if (isLoading) {
            wasLoading = true
        } else if (wasLoading) {
            wasLoading = false
            if (errorMessage == null) {
                Toast.makeText(context, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                onBack() // Ahora SÍ volvemos de forma segura
            } else {
                Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                authViewModel.setErrorMessage(null)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isLoading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            authViewModel.updateProfile(
                                newName = name,
                                newImageUri = selectedImageUri,
                                bio = bio.takeIf { it.isNotBlank() },
                                birthday = birthday.takeIf { it.isNotBlank() },
                                mangaInterests = selectedGenres.toList()
                            )
                            // ELIMINAMOS EL onBack() DE AQUÍ PARA EVITAR CORTAR LA SUBIDA
                        },
                        enabled = name.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Profile Picture
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = !isLoading) { // Deshabilita clics si está cargando
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Nueva Foto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (userProfile?.photoUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = userProfile.photoUrl,
                            contentDescription = "Foto Actual",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Añadir Foto", modifier = Modifier.size(40.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ID Section
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ID de Shiori", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(userProfile?.userId ?: "", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Shiori ID", userProfile?.userId ?: "")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "ID copiado", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar ID", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "¡Añádeme en ShioriApp! Mi ID es: ${userProfile?.userId}\nO usa este link: shioriapp://user/${userProfile?.userId}")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Compartir ID de Shiori")
                            context.startActivity(shareIntent)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir Link")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Form Fields
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Apodo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading // Se bloquea al guardar
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Sobre mí (Bio)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = birthday,
                    onValueChange = { birthday = it },
                    label = { Text("Fecha de nacimiento (Ej: 15/04/1998)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Interests Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Intereses en Mangas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableGenres.forEach { genre ->
                            val isSelected = selectedGenres.contains(genre)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (!isLoading) {
                                        selectedGenres = if (isSelected) {
                                            selectedGenres - genre
                                        } else {
                                            selectedGenres + genre
                                        }
                                    }
                                },
                                label = { Text(genre) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}