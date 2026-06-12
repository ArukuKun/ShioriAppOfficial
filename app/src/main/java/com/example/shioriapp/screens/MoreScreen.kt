package com.example.shioriapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
){
    val isDarkMode = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {

        // --- LOGO DINÁMICO ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp),
            contentAlignment = Alignment.Center
        ) {
            val logoResource = if (isDarkMode) {
                R.drawable.ic_shiori_black
            } else {
                R.drawable.ic_shiori_white
            }

            Image(
                painter = painterResource(id = logoResource),
                contentDescription = "Logo de ShioriApp",
                modifier = Modifier.size(200.dp)
            )
        }

        SettingsSectionTitle(title = "Integraciones")
        SettingsItem(
            icon = Icons.Default.MusicNote,
            title = "Spotify",
            subtitle = "Vincula tu cuenta para escuchar OSTs",
            onClick = { /* TODO: Lógica de Login de Spotify */ }
        )
        SettingsItem(
            icon = Icons.Default.Sync,
            title = "AnimeTrack / AniList",
            subtitle = "Sincroniza tus capítulos leídos"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECCIÓN 2: FUENTES ---
        SettingsSectionTitle(title = "Fuentes")
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

        Spacer(modifier = Modifier.height(110.dp))
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