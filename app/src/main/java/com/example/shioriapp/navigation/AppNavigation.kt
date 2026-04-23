package com.example.shioriapp.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.shioriapp.screens.*
import com.example.shioriapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (currentRoute != "settings") {
                CenterAlignedTopAppBar(
                    title = {
                        if (currentRoute == "home") {
                            // Detectamos el tema actual del sistema
                            val isDarkMode = isSystemInDarkTheme()
                            val logoResource = if (isDarkMode) {
                                R.drawable.ic_shiori_white
                            } else {
                                R.drawable.ic_shiori_black
                            }

                            // Mostramos el logo
                            Image(
                                painter = painterResource(id = logoResource),
                                contentDescription = "Logo de ShioriApp",
                                modifier = Modifier.height(40.dp) // Ajusta este valor si lo ves muy grande o pequeño
                            )
                        } else {
                            // Para las demás pantallas mantenemos el texto
                            val titleText = when(currentRoute) {
                                "explore" -> "Explorar"
                                else -> "ShioriApp"
                            }
                            Text(text = titleText)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    actions = {
                        IconButton(onClick = { /* Acción buscar */ }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                        // Nuevo icono de notificación a la derecha
                        IconButton(onClick = { /* Acción notificación */ }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notificaciones")
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Biblioteca") },
                    label = { Text("Biblioteca") },
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = "Explorar") },
                    label = { Text("Explorar") },
                    selected = currentRoute == "explore",
                    onClick = { navController.navigate("explore") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Más") },
                    label = { Text("Más") },
                    selected = currentRoute == "más",
                    onClick = { navController.navigate("más") }
                )
            }
        }
    ) { paddingValues ->

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") { HomeScreen() }
            composable("explore") { ExploreScreen() }
            composable("settings") {
                SettingsScreen(
                    onNavigateToRepository = {
                        navController.navigate("repository")
                    }
                )
            }
            composable("repository") {
                RepositoryScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}