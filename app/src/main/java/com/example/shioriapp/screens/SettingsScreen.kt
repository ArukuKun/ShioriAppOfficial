package com.example.shioriapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAppearance: () -> Unit
) {
    val isDarkMode = isSystemInDarkTheme()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            // --- GENERAL ---
            SettingsSectionTitle(title = "General")
            SettingsItem(
                icon = Icons.Default.ColorLens,
                title = "Apariencia",
                subtitle = if (isDarkMode) "Tema actual: Oscuro" else "Tema actual: Claro",
                onClick = { onNavigateToAppearance() }
            )
            SettingsItem(
                icon = Icons.Default.Language,
                title = "Idioma",
                subtitle = "Español"
            )
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "Notificaciones",
                subtitle = "Avisos de nuevos capítulos y actualizaciones"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- LECTOR ---
            SettingsSectionTitle(title = "Lector")
            SettingsItem(
                icon = Icons.Default.MenuBook,
                title = "Ajustes del Lector",
                subtitle = "Orientación, color de fondo y modo de toque"
            )
            SettingsItem(
                icon = Icons.Default.FilterBAndW,
                title = "Filtro de Color",
                subtitle = "Ajusta el brillo y los filtros de las páginas"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- AVANZADO ---
            SettingsSectionTitle(title = "Avanzado")
            SettingsItem(
                icon = Icons.Default.Cached,
                title = "Limpiar Caché",
                subtitle = "Libera espacio borrando imágenes temporales"
            )
            SettingsItem(
                icon = Icons.Default.Cookie,
                title = "Cookies y Webview",
                subtitle = "Limpiar datos de navegación de las fuentes"
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}