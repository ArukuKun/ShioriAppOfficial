package com.example.shioriapp.screens

import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun SettingsScreen(
    onNavigateToRepository: () -> Unit = {}
) {
    // 🔥 Creamos un NavController interno SOLO para los ajustes
    val settingsNavController = rememberNavController()

    NavHost(
        navController = settingsNavController,
        startDestination = "menu_principal",
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
    ) {
        composable("menu_principal") {
            MainMenuScreen(settingsNavController, onNavigateToRepository)
        }
        composable("almacenamiento") {
            StorageScreen(onBack = { settingsNavController.popBackStack() })
        }
        composable("cuenta") {
            AccountScreen(onBack = { settingsNavController.popBackStack() })
        }
        composable("integraciones") {
            IntegrationsScreen(onBack = { settingsNavController.popBackStack() })
        }
        // Puedes agregar "general", "lector" y "shioriai" como pantallas independientes después
    }
}

// ── 1. MENÚ PRINCIPAL (SIN FLECHA DE VOLVER) ─────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(navController: NavController, onNavigateToRepository: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", fontWeight = FontWeight.Bold) },
                // Eliminamos insets innecesarios para que suba y quede alineado
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item { SettingsCategoryHeader("Perfil y Red") }
            item {
                SettingsNavigationItem(
                    title = "Cuenta y Sincronización",
                    subtitle = "Respaldo en la nube, perfiles vinculados",
                    icon = Icons.Default.AccountCircle,
                    onClick = { navController.navigate("cuenta") }
                )
            }
            item {
                SettingsNavigationItem(
                    title = "Integraciones",
                    subtitle = "Discord, Spotify y servicios externos",
                    icon = Icons.Default.Hub,
                    onClick = { navController.navigate("integraciones") }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item { SettingsCategoryHeader("Aplicación") }
            item {
                SettingsNavigationItem(
                    title = "Almacenamiento",
                    subtitle = "Espacio libre, descargas y caché",
                    icon = Icons.Default.Storage,
                    onClick = { navController.navigate("almacenamiento") }
                )
            }
            item {
                SettingsNavigationItem(
                    title = "Repositorios y Extensiones",
                    subtitle = "Administrar fuentes de manga instaladas",
                    icon = Icons.Default.Extension,
                    onClick = onNavigateToRepository
                )
            }
            item {
                SettingsNavigationItem(
                    title = "Apariencia y General",
                    subtitle = "Temas, idiomas y comportamiento",
                    icon = Icons.Default.Palette,
                    onClick = { /* Navegar a Apariencia */ }
                )
            }
            item {
                SettingsNavigationItem(
                    title = "Lector",
                    subtitle = "Paginación, pantalla y navegación",
                    icon = Icons.Default.MenuBook,
                    onClick = { /* Navegar a Lector */ }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item { SettingsCategoryHeader("Experimental") }
            item {
                SettingsNavigationItem(
                    title = "ShioriAI",
                    subtitle = "Asistente inteligente y contexto",
                    icon = Icons.Default.AutoAwesome,
                    onClick = { /* Navegar a AI */ }
                )
            }
        }
    }
}

// ── 2. PANTALLA DE ALMACENAMIENTO (CÁLCULO REAL) ─────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(onBack: () -> Unit) {
    // Cálculo real del almacenamiento del teléfono
    val stat = StatFs(Environment.getDataDirectory().path)
    val bytesTotal = stat.blockSizeLong * stat.blockCountLong
    val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
    val bytesUsed = bytesTotal - bytesAvailable

    val totalGb = bytesTotal / (1024 * 1024 * 1024).toFloat()
    val usedGb = bytesUsed / (1024 * 1024 * 1024).toFloat()
    val percentageUsed = bytesUsed.toFloat() / bytesTotal.toFloat()

    // Mock del espacio de Shiori
    val shioriDownloadsGb = 1.2f
    val shioriCacheMb = 350f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Almacenamiento", fontWeight = FontWeight.Medium, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Espacio del dispositivo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            // Barra visual de almacenamiento
            LinearProgressIndicator(
                progress = { percentageUsed },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(String.format("%.1f GB usados", usedGb), fontSize = 14.sp)
                Text(String.format("%.1f GB totales", totalGb), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Desglose de la aplicación", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            StorageInfoRow("Mangas Descargados", String.format("%.1f GB", shioriDownloadsGb), Icons.Default.Download)
            StorageInfoRow("Caché de Imágenes", String.format("%.0f MB", shioriCacheMb), Icons.Default.Image)
            StorageInfoRow("Base de Datos (Librería)", "12 MB", Icons.Default.DataUsage)

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { /* Lógica de limpiar caché */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Limpiar Caché y Temporales")
            }
        }
    }
}

@Composable
fun StorageInfoRow(title: String, size: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), fontSize = 16.sp)
        Text(size, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

// ── 3. PANTALLA DE CUENTA Y SINCRONIZACIÓN ───────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cuenta", fontWeight = FontWeight.Medium, fontSize = 20.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") } },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            item { SettingsCategoryHeader("Sincronización de Lectura") }
            item {
                SettingsClickableItem("Vincular con AniList", "Sincroniza tus capítulos leídos automáticamente", Icons.Default.Sync) {}
            }
            item {
                SettingsClickableItem("Vincular con MyAnimeList", "No conectado", Icons.Default.Link) {}
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item { SettingsCategoryHeader("Copia de Seguridad (Nube)") }
            item {
                SettingsClickableItem("Conectar Google Drive", "Respalda tu biblioteca de mangas y categorías", Icons.Default.CloudUpload) {}
            }
        }
    }
}

// ── 4. PANTALLA DE INTEGRACIONES (DISCORD/SPOTIFY) ───────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationsScreen(onBack: () -> Unit) {
    var discordRpc by remember { mutableStateOf(false) }
    var spotifyInt by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Integraciones", fontWeight = FontWeight.Medium, fontSize = 20.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") } },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            item { SettingsCategoryHeader("Juegos y Estado") }
            item {
                SettingsSwitchItem(
                    title = "Discord Rich Presence",
                    subtitle = "Muestra el manga y capítulo que estás leyendo en tu estado de Discord",
                    icon = Icons.Default.SportsEsports,
                    checked = discordRpc,
                    onCheckedChange = { discordRpc = it }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item { SettingsCategoryHeader("Música y Audio") }
            item {
                SettingsSwitchItem(
                    title = "Integración con Spotify",
                    subtitle = "Muestra controles flotantes y pausas automáticas según la lectura",
                    icon = Icons.Default.MusicNote,
                    checked = spotifyInt,
                    onCheckedChange = { spotifyInt = it }
                )
            }
        }
    }
}

// ── COMPONENTES VISUALES BASE ────────────────────────────────────────────────
@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp)
            .fillMaxWidth()
    )
}

@Composable
fun SettingsNavigationItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = "Ir", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun SettingsClickableItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun SettingsSwitchItem(
    title: String, subtitle: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}