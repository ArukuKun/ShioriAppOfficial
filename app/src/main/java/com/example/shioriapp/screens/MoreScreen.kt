package com.example.shioriapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.shioriapp.R
import com.example.shioriapp.viewmodel.AuthViewModel

@Composable
fun MoreScreen(
    authViewModel: AuthViewModel,
    onNavigateToExtension: () -> Unit,
    onNavigateToMigration: () -> Unit,
    onNavigateToStorage: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // --- HEADER JAPONÉS MINIMALISTA ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                when (val state = authState) {
                    is AuthViewModel.AuthState.Authenticated -> {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .padding(2.dp)
                        ) {
                            AsyncImage(
                                model = state.profile?.photoUrl ?: R.drawable.ic_shiori_black,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = state.profile?.displayName ?: "Usuario",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = state.profile?.email ?: "",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                Text(
                                    text = "PRO MEMBER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (state is AuthViewModel.AuthState.Guest) "Invitado" else "No autenticado",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { authViewModel.logout() },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Inicia sesión para sincronizar →", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            
            SettingsSectionTitle(title = "APLICACIÓN")
            
            SettingsItem(
                icon = Icons.Default.Extension,
                title = "Extensiones",
                subtitle = "Gestiona tus fuentes de lectura",
                onClick = onNavigateToExtension
            )

            SettingsItem(
                icon = Icons.Default.ImportExport,
                title = "Migración de Biblioteca",
                subtitle = "Mover mangas entre extensiones",
                onClick = onNavigateToMigration
            )

            SettingsItem(
                icon = Icons.Default.Storage,
                title = "Almacenamiento",
                subtitle = "Caché, descargas y base de datos",
                onClick = onNavigateToStorage
            )

            SettingsSectionTitle(title = "PERSONALIZACIÓN")
            SettingsItem(icon = Icons.Default.Palette, title = "Apariencia", subtitle = "Temas y modo oscuro")
            SettingsItem(icon = Icons.Default.Notifications, title = "Notificaciones", subtitle = "Alertas de capítulos")
            SettingsItem(icon = Icons.Default.Language, title = "Idioma", subtitle = "Configuración regional")

            SettingsSectionTitle(title = "CUENTA")
            if (authState is AuthViewModel.AuthState.Authenticated) {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Cerrar sesión",
                    subtitle = "Desvincular cuenta de Google",
                    onClick = { authViewModel.logout() },
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                SettingsItem(
                    icon = Icons.Default.Login,
                    title = "Iniciar sesión",
                    subtitle = "Accede a tus datos en la nube",
                    onClick = { authViewModel.logout() }
                )
            }

            SettingsSectionTitle(title = "SOPORTE")
            SettingsItem(icon = Icons.Default.Info, title = "ShioriApp v1.0.0", subtitle = "Ver notas de la versión")
            
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 28.dp, bottom = 8.dp, start = 12.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {},
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (color == MaterialTheme.colorScheme.error) color else Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (color == MaterialTheme.colorScheme.error) color else Color.White,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
