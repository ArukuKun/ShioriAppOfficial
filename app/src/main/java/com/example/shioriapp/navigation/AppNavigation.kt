package com.example.shioriapp.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.shioriapp.R
import com.example.shioriapp.screens.*
import com.example.shioriapp.viewmodel.SharedReaderViewModel
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val MAIN_TABS = "main_tabs"
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val MENSAJERIA = "mensajeria"
    const val MAS = "mas"
    const val REPOSITORY = "repository"
    const val DETAILS = "manga_details/{sourceName}?mangaUrl={mangaUrl}&mangaTitle={mangaTitle}"
    const val READER = "reader/{sourceName}"
    const val SEARCH = "search" // 🔥 Nueva Ruta
}

@Composable
fun AppNavigation() {
    val rootNavController = rememberNavController()
    val sharedReaderViewModel: SharedReaderViewModel = viewModel()

    NavHost(
        navController = rootNavController,
        startDestination = Routes.MAIN_TABS,
        modifier = Modifier.fillMaxSize().background(Color.Black),
        enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(300)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
    ) {
        composable(Routes.MAIN_TABS) {
            MainTabsScreen(rootNavController)
        }

        composable(
            route = Routes.DETAILS,
            arguments = listOf(
                navArgument("sourceName") { type = NavType.StringType },
                navArgument("mangaUrl") { type = NavType.StringType; nullable = true; defaultValue = "" },
                navArgument("mangaTitle") { type = NavType.StringType; nullable = true; defaultValue = "" }
            )
        ) { backStackEntry ->
            val sourceName = URLDecoder.decode(backStackEntry.arguments?.getString("sourceName") ?: "", "UTF-8")
            val encodedUrl = backStackEntry.arguments?.getString("mangaUrl") ?: ""
            val encodedTitle = backStackEntry.arguments?.getString("mangaTitle") ?: ""

            val mangaUrl = URLDecoder.decode(encodedUrl, "UTF-8")
            val mangaTitle = URLDecoder.decode(encodedTitle, "UTF-8")

            MangaDetailsScreen(
                mangaUrl = mangaUrl,
                sourceName = sourceName,
                mangaTitle = mangaTitle,
                onBack = { rootNavController.popBackStack() },
                onChapterClick = { chapter, chapters ->
                    sharedReaderViewModel.currentChapter = chapter
                    sharedReaderViewModel.chapters = chapters

                    val safeSource = if (sourceName.isNotBlank()) sourceName else "FuenteDesconocida"
                    val encSource = URLEncoder.encode(safeSource, "UTF-8")

                    rootNavController.navigate("reader/$encSource")
                },
                onCategoryClick = { /* ... */ }
            )
        }

        composable(
            route = Routes.READER,
            arguments = listOf(navArgument("sourceName") { type = NavType.StringType })
        ) { backStackEntry ->
            val encSource = backStackEntry.arguments?.getString("sourceName") ?: ""
            val decodedSource = URLDecoder.decode(encSource, "UTF-8")

            ReaderScreen(
                sourceName = decodedSource,
                sharedViewModel = sharedReaderViewModel,
                onBack = { rootNavController.popBackStack() }
            )
        }

        composable(Routes.REPOSITORY) {
            ExtensionsScreen(onBack = { rootNavController.popBackStack() })
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { rootNavController.popBackStack() },
                onMangaClick = { manga ->
                    val encUrl = URLEncoder.encode(manga.url, "UTF-8")
                    val encTitle = URLEncoder.encode(manga.title, "UTF-8")
                    val encSource = URLEncoder.encode(manga.sourceName, "UTF-8")
                    rootNavController.navigate("manga_details/$encSource?mangaUrl=$encUrl&mangaTitle=$encTitle")
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabsScreen(rootNavController: NavHostController) {
    val tabsNavController = rememberNavController()
    val navBackStackEntry by tabsNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.HOME
    var showNotifications by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    when (currentRoute) {
                        Routes.HOME -> {
                            val logoRes = if (isSystemInDarkTheme()) R.drawable.ic_shiori_white else R.drawable.ic_shiori_black
                            Image(painter = painterResource(id = logoRes), contentDescription = null, modifier = Modifier.height(40.dp))
                        }
                        Routes.EXPLORE -> Text("Explorar", fontWeight = FontWeight.Bold)
                        Routes.MENSAJERIA -> Text("Mensajería", fontWeight = FontWeight.Bold)
                        else -> Text("ShioriApp")
                    }
                },
                actions = {
                    // 🔥 Conecta el botón a la búsqueda global
                    IconButton(onClick = { rootNavController.navigate(Routes.SEARCH) }) {
                        Icon(Icons.Default.Search, "Buscar")
                    }
                    IconButton(onClick = { showNotifications = !showNotifications }) {
                        Icon(Icons.Default.Notifications, "Notificaciones")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                val items = listOf(
                    Triple(Routes.HOME, Icons.Default.Home, "Biblioteca"),
                    Triple(Routes.EXPLORE, Icons.Default.Explore, "Explorar"),
                    Triple(Routes.MENSAJERIA, Icons.Default.ChatBubbleOutline, "Mensajes"),
                    Triple(Routes.MAS, Icons.Default.MoreHoriz, "Más")
                )
                items.forEach { (route, icon, label) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = currentRoute == route,
                        onClick = {
                            if (currentRoute != route) {
                                tabsNavController.navigate(route) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabsNavController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(0)) },
            exitTransition = { fadeOut(tween(0)) }
        ) {
            composable(Routes.HOME) {
                HomeScreen(onMangaClick = { manga ->
                    val encUrl = URLEncoder.encode(manga.url, "UTF-8")
                    val encTitle = URLEncoder.encode(manga.title, "UTF-8")
                    val encSource = URLEncoder.encode(manga.sourceName, "UTF-8")
                    rootNavController.navigate("manga_details/$encSource?mangaUrl=$encUrl&mangaTitle=$encTitle")
                })
            }
            composable(Routes.EXPLORE) {
                ExploreScreen(onMangaClick = { url, source, title ->
                    val encUrl    = URLEncoder.encode(url,    "UTF-8")
                    val encTitle  = URLEncoder.encode(title,  "UTF-8")
                    val encSource = URLEncoder.encode(source, "UTF-8")
                    rootNavController.navigate("manga_details/$encSource?mangaUrl=$encUrl&mangaTitle=$encTitle")
                })
            }
            composable(Routes.MENSAJERIA) { MensajeriaScreen() }
            composable(Routes.MAS) {
                SettingsScreen(onNavigateToRepository = { rootNavController.navigate(Routes.REPOSITORY) })
            }
        }

        if (showNotifications) {
            NotificationDropdown(expanded = showNotifications, onDismiss = { showNotifications = false })
        }
    }
}

@Composable
fun NotificationDropdown(expanded: Boolean, onDismiss: () -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(280.dp).background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Notificaciones", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Icon(Icons.Default.NotificationsOff, null, tint = MaterialTheme.colorScheme.primary.copy(0.6f), modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("No tienes ninguna notificación.", fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}