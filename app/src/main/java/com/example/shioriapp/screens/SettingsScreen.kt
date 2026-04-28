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

@Composable
fun SettingsScreen(
    // CORRECCIÓN: Definimos el parámetro para que la navegación funcione
    onNavigateToRepository: () -> Unit
) {
    val isDarkMode = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {

        // --- LOGO DINÁMICO ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            val logoResource = if (isDarkMode) {
                R.drawable.ic_shiori_white
            } else {
                R.drawable.ic_shiori_black
            }

            Image(
                painter = painterResource(id = logoResource),
                contentDescription = "Logo de ShioriApp",
                modifier = Modifier.size(160.dp)
            )
        }

        SettingsSectionTitle(title = "Sincronización")
        SettingsItem(
            icon = Icons.Default.Sync,
            title = "AnimeTrack / AniList",
            subtitle = "Sincroniza tus capítulos leídos"
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSectionTitle(title = "Fuentes")

        // --- REPOSITORIOS (Navegación corregida) ---
        SettingsItem(
            icon = Icons.Default.CloudDownload,
            title = "Extensiones",
            subtitle = "Gestiona las extensiones de mangas",
            onClick = { onNavigateToRepository() }
        )

        SettingsItem(
            icon = Icons.Default.CloudDownload,
            title = "Repositorios",
            subtitle = "Gestiona los repositorios añadidos",
            onClick = { onNavigateToRepository() }
        )


        SettingsItem(
            icon = Icons.Default.Storage,
            title = "Almacenamiento",
            subtitle = "Limpiar caché y mangas descargados"
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSectionTitle(title = "General")
        SettingsItem(
            icon = Icons.Default.ColorLens,
            title = "Apariencia",
            subtitle = if (isDarkMode) "Modo Oscuro activado" else "Modo Claro activado"
        )
        SettingsItem(
            icon = Icons.Default.Notifications,
            title = "Notificaciones",
            subtitle = "Avisos de nuevos capítulos"
        )

        Spacer(modifier = Modifier.height(32.dp))
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