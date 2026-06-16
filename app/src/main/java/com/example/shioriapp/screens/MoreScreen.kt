package com.example.shioriapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shioriapp.R
import com.example.shioriapp.navigation.Routes
import com.example.shioriapp.viewmodel.AuthViewModel

@Composable
fun MoreScreen(
    authViewModel: AuthViewModel,
    onNavigateToExtension: () -> Unit,
    onNavigateToRepository: () -> Unit,
    onNavigateToMigration: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogin: () -> Unit = {}
){
    val isDarkMode = isSystemInDarkTheme()
    val authState by authViewModel.authState.collectAsState()
    val isAuthenticated = authState is AuthViewModel.AuthState.Authenticated
    val userProfile = (authState as? AuthViewModel.AuthState.Authenticated)?.profile

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // 🔥 SECCIÓN DE PERFIL DE USUARIO
        if (isAuthenticated && userProfile != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Perfil",
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userProfile.displayName ?: "Usuario",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = userProfile.email ?: "Sin correo",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Botón de cerrar sesión más compacto y elegante
                    OutlinedButton(
                        onClick = { authViewModel.logout() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cerrar Sesión", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        } else {
            // 🔥 LOGO RESPONSIVO (Solo si no está autenticado)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                val logoSize = when {
                    maxWidth < 400.dp -> 100.dp
                    maxWidth < 600.dp -> 120.dp
                    else -> 150.dp
                }

                val logoResource = if (isDarkMode) {
                    R.drawable.ic_shiori_black
                } else {
                    R.drawable.ic_shiori_white
                }

                Image(
                    painter = painterResource(id = logoResource),
                    contentDescription = "Logo de ShioriApp",
                    modifier = Modifier.size(logoSize)
                )
            }
            
            // Botón de iniciar sesión más compacto
            OutlinedButton(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(20.dp).padding(end = 8.dp))
                Text("Iniciar Sesión", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        SettingsSectionTitle(title = "Fuentes y Contenido")
        SettingsItem(
            icon = Icons.Default.CloudDownload,
            title = "Extensiones",
            subtitle = "Gestiona las extensiones de mangas",
            onClick = { onNavigateToExtension() }
        )
        SettingsItem(
            icon = Icons.Default.FolderSpecial,
            title = "Repositorios",
            subtitle = "Gestiona los repositorios añadidos",
            onClick = { onNavigateToRepository() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSectionTitle(title = "Herramientas")
        SettingsItem(
            icon = Icons.Default.ImportExport,
            title = "Migrar",
            subtitle = "Migra mangas de una extensión a otra",
            onClick = { onNavigateToMigration() }
        )
        SettingsItem(
            icon = Icons.Default.Storage,
            title = "Gestor de Descargas",
            subtitle = "Ver y organizar tus mangas descargados",
            onClick = { onNavigateToStorage() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSectionTitle(title = "Sistema")
        SettingsItem(
            icon = Icons.Default.Settings,
            title = "Configuración",
            subtitle = "Apariencia, lector, notificaciones y caché",
            onClick = { onNavigateToSettings() }
        )
        
        if (isAuthenticated) {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Acerca de",
                subtitle = "Versión de la aplicación y licencias",
                onClick = { /* TODO: Navegar a pantalla Acerca de */ }
            )
        }

        // 🔥 Espacio para la barra inferior flotante
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}