package com.example.shioriapp.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.shioriapp.R
import com.example.shioriapp.screens.ShioriAI
import com.example.shioriapp.screens.ExploreScreen
import com.example.shioriapp.screens.ExtensionsScreen
import com.example.shioriapp.screens.HomeScreen
import com.example.shioriapp.screens.MensajeriaScreen
import com.example.shioriapp.screens.SettingsScreen

private object Routes {
    const val HOME       = "home"
    const val EXPLORE    = "explore"
    const val SHIORI     = "shiori"
    const val MENSAJERIA = "mensajeria"
    const val MAS        = "mas"
    const val REPOSITORY = "repository"
}

private val routesWithoutTopBar    = setOf(Routes.MAS, Routes.REPOSITORY)
private val routesWithoutBottomBar = setOf(Routes.REPOSITORY)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showNotifications by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = if (currentRoute == Routes.REPOSITORY)
            WindowInsets(0.dp)
        else
            ScaffoldDefaults.contentWindowInsets,

        topBar = {
            if (currentRoute !in routesWithoutTopBar) {
                CenterAlignedTopAppBar(
                    title = {
                        when (currentRoute) {
                            Routes.HOME -> {
                                val logoRes = if (isSystemInDarkTheme())
                                    R.drawable.ic_shiori_white
                                else
                                    R.drawable.ic_shiori_black
                                Image(
                                    painter = painterResource(id = logoRes),
                                    contentDescription = "Logo de ShioriApp",
                                    modifier = Modifier.height(40.dp)
                                )
                            }
                            Routes.EXPLORE    -> Text("Buscar")
                            Routes.SHIORI     -> Text("ShioriAI")
                            Routes.MENSAJERIA -> Text("Mensajería")
                            else              -> Text("ShioriApp")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    actions = {
                        IconButton(onClick = { /* TODO: Buscar global */ }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }

                        Box {
                            IconButton(onClick = { showNotifications = !showNotifications }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notificaciones")
                            }

                            DropdownMenu(
                                expanded = showNotifications,
                                onDismissRequest = { showNotifications = false },
                                modifier = Modifier
                                    .width(280.dp)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Notificaciones",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(modifier = Modifier.height(24.dp))

                                    Icon(
                                        imageVector = Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "No tienes ninguna notificación por el momento.",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                )
            }
        },

        bottomBar = {
            if (currentRoute !in routesWithoutBottomBar) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Biblioteca") },
                        label = { Text("Biblioteca") },
                        selected = currentRoute == Routes.HOME,
                        onClick = { navController.navigate(Routes.HOME) }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                        label = { Text("Buscar") },
                        selected = currentRoute == Routes.EXPLORE,
                        onClick = { navController.navigate(Routes.EXPLORE) }
                    )

                    // ✅ IA SEMI-OCULTA: Sin bóton saltón y usando un icono discreto
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Assistant, contentDescription = "ShioriAI") },
                        label = { Text("ShioriAI") },
                        selected = currentRoute == Routes.SHIORI,
                        onClick = { navController.navigate(Routes.SHIORI) }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Mensajería") },
                        label = { Text("Mensajes") },
                        selected = currentRoute == Routes.MENSAJERIA,
                        onClick = { navController.navigate(Routes.MENSAJERIA) }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "Más") },
                        label = { Text("Más") },
                        selected = currentRoute == Routes.MAS,
                        onClick = { navController.navigate(Routes.MAS) }
                    )
                }
            }
        }

    ) { paddingValues ->

        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = if (currentRoute == Routes.REPOSITORY)
                Modifier.fillMaxSize()
            else
                Modifier.padding(paddingValues)
        ) {
            composable(Routes.HOME)       { HomeScreen() }
            composable(Routes.EXPLORE)    { ExploreScreen() }
            composable(Routes.SHIORI)     { ShioriAI() }
            composable(Routes.MENSAJERIA) { MensajeriaScreen() }
            composable(Routes.MAS) {
                SettingsScreen(
                    onNavigateToRepository = { navController.navigate(Routes.REPOSITORY) }
                )
            }
            composable(Routes.REPOSITORY) {
                ExtensionsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}