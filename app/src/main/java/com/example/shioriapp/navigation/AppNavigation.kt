package com.example.shioriapp.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.shioriapp.R
import com.example.shioriapp.data.repository.LibraryManager
import com.example.shioriapp.domain.model.ChapterInfo
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.screens.*
import org.json.JSONArray
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

object Routes {
    const val MAIN_TABS = "main_tabs"
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val MENSAJERIA = "mensajeria"
    const val MAS = "mas"
    const val STORAGE_SETTINGS = "storage_settings"
    const val EXTENSION = "extension"
    const val DETAILS = "manga_details/{sourceName}?mangaUrl={mangaUrl}&mangaTitle={mangaTitle}&resume={resume}"
    const val SEARCH = "search"
    const val MIGRATE = "migrate_screen"
    const val MIGRATE_SEARCH = "migrate_search/{query}"
}

object ReaderDataCache {
    var currentChapter: ChapterInfo? = null
    var chapters: List<ChapterInfo> = emptyList()
    var mangaUrl: String = ""
}

object MigrationCache {
    var oldManga: MangaInfo? = null
}

@Composable
fun AppNavigation() {
    val rootNavController = rememberNavController()
    val context = LocalContext.current

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
                navArgument("mangaTitle") { type = NavType.StringType; nullable = true; defaultValue = "" },
                navArgument("resume") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("mangaUrl") ?: ""
            val encodedTitle = backStackEntry.arguments?.getString("mangaTitle") ?: ""
            val resume = backStackEntry.arguments?.getBoolean("resume") ?: false

            val sourceName = safeUrlDecode(backStackEntry.arguments?.getString("sourceName") ?: "")
            val mangaUrl = safeUrlDecode(encodedUrl)
            val mangaTitle = safeUrlDecode(encodedTitle)

            var showFolderPickerDialog by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            val folderPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri: Uri? ->
                if (uri != null) {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    val prefs = context.getSharedPreferences("shiori_storage", Context.MODE_PRIVATE)
                    prefs.edit().putString("download_path", uri.toString()).apply()
                    android.widget.Toast.makeText(context, "Carpeta guardada. Ya puedes descargar.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            if (showFolderPickerDialog) {
                AlertDialog(
                    onDismissRequest = { showFolderPickerDialog = false },
                    title = { Text("Carpeta de descargas") },
                    text = { Text("No has seleccionado dónde guardar los mangas. ¿Deseas elegir una carpeta ahora?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showFolderPickerDialog = false
                            folderPickerLauncher.launch(null)
                        }) { Text("Elegir carpeta", fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = { TextButton(onClick = { showFolderPickerDialog = false }) { Text("Cancelar") } }
                )
            }

            MangaDetailsScreen(
                mangaUrl = mangaUrl,
                sourceName = sourceName,
                mangaTitle = mangaTitle,
                autoResume = resume,
                onBack = { rootNavController.popBackStack() },
                onChapterClick = { chapter, chapters ->
                    ReaderDataCache.currentChapter = chapter
                    ReaderDataCache.chapters = chapters
                    ReaderDataCache.mangaUrl = mangaUrl
                    val safeSource = if (sourceName.isNotBlank()) sourceName else "FuenteDesconocida"
                    val intent = Intent(context, ReaderActivity::class.java).apply { putExtra("sourceName", safeSource) }
                    context.startActivity(intent)
                },
                onCategoryClick = { rootNavController.navigate(Routes.MAIN_TABS) { popUpTo(Routes.MAIN_TABS) { inclusive = false } } },
                onMigrateClick = { currentManga ->
                    MigrationCache.oldManga = currentManga
                    rootNavController.navigate("migrate_search/${URLEncoder.encode(currentManga.title, "UTF-8")}")
                },
                onDownloadChapter = { chapter ->
                    val prefs = context.getSharedPreferences("shiori_storage", Context.MODE_PRIVATE)
                    val folderUriString = prefs.getString("download_path", null)

                    if (!folderUriString.isNullOrBlank() && folderUriString.startsWith("content://")) {
                        scope.launch(Dispatchers.IO) {
                            com.example.shioriapp.core.util.MangaDownloader.downloadChapter(
                                context, folderUriString, mangaTitle, chapter
                            )
                        }
                    } else {
                        showFolderPickerDialog = true
                    }
                },
                onDownloadAll = { allChapters ->
                    val prefs = context.getSharedPreferences("shiori_storage", Context.MODE_PRIVATE)
                    val folderUriString = prefs.getString("download_path", null)

                    if (!folderUriString.isNullOrBlank() && folderUriString.startsWith("content://")) {
                        scope.launch(Dispatchers.IO) {
                            allChapters.forEach { chapter ->
                                com.example.shioriapp.core.util.MangaDownloader.downloadChapter(
                                    context, folderUriString, mangaTitle, chapter
                                )
                            }
                        }
                    } else {
                        showFolderPickerDialog = true
                    }
                },
                onDeleteChapter = { chapter ->
                    val prefs = context.getSharedPreferences("shiori_storage", Context.MODE_PRIVATE)
                    val folderUriString = prefs.getString("download_path", null)

                    if (!folderUriString.isNullOrBlank()) {
                        scope.launch(Dispatchers.IO) {
                            com.example.shioriapp.core.util.MangaDownloader.deleteChapter(
                                context, folderUriString, mangaTitle, chapter.name
                            )
                        }
                        android.widget.Toast.makeText(context, "Descarga eliminada", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        composable(Routes.EXTENSION) {
            ExtensionsScreen(onBack = { rootNavController.popBackStack() })
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { rootNavController.popBackStack() },
                onMangaClick = { manga ->
                    val encUrl = URLEncoder.encode(manga.url, "UTF-8")
                    val encTitle = URLEncoder.encode(manga.title, "UTF-8")
                    val encSource = URLEncoder.encode(manga.sourceName, "UTF-8")
                    rootNavController.navigate("manga_details/$encSource?mangaUrl=$encUrl&mangaTitle=$encTitle&resume=false")
                }
            )
        }

        composable(Routes.MIGRATE) {
            MigrateScreen(
                onBack = { rootNavController.popBackStack() },
                onNavigateToSearch = { query ->
                    rootNavController.navigate("migrate_search/${URLEncoder.encode(query, "UTF-8")}")
                }
            )
        }

        composable(
            route = Routes.MIGRATE_SEARCH,
            arguments = listOf(navArgument("query") { type = NavType.StringType })
        ) { backStackEntry ->
            val query = URLDecoder.decode(backStackEntry.arguments?.getString("query") ?: "", "UTF-8")
            MigrateSearchScreen(
                query = query,
                onBack = { rootNavController.popBackStack() },
                onMigrationComplete = {
                    rootNavController.navigate(Routes.MAIN_TABS) {
                        popUpTo(Routes.MAIN_TABS) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.STORAGE_SETTINGS) {
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("shiori_storage", Context.MODE_PRIVATE)
            var savedPath by remember { mutableStateOf(prefs.getString("download_path", "Descargas internas") ?: "") }

            StorageSettingsScreen(
                currentPath = savedPath,
                onBack = { rootNavController.popBackStack() },
                onFolderChange = { newUri ->
                    context.contentResolver.takePersistableUriPermission(newUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    prefs.edit().putString("download_path", newUri.toString()).apply()
                    savedPath = newUri.lastPathSegment ?: "Nueva carpeta"
                }
            )
        }
    }
}

// TopAppBar sin modificaciones
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    currentRoute: String,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
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
            IconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, "Buscar") }
            IconButton(onClick = onNotificationsClick) { Icon(Icons.Default.Notifications, "Notificaciones") }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabsScreen(rootNavController: NavHostController) {
    val tabsNavController = rememberNavController()
    val navBackStackEntry by tabsNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.HOME
    var showNotifications by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
            modifier = Modifier
                .fillMaxSize()
                // 🔥 EL FIX ESTÁ AQUÍ: Solo tomamos el padding de abajo.
                // Ignoramos el top padding porque la MainTopAppBar ya hace ese empuje sola.
                .padding(bottom = innerPadding.calculateBottomPadding()),
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) }
        ) {
            composable(Routes.HOME) {
                Column(modifier = Modifier.fillMaxSize()) {
                    MainTopAppBar(
                        currentRoute = Routes.HOME,
                        onSearchClick = { rootNavController.navigate(Routes.SEARCH) },
                        onNotificationsClick = { showNotifications = !showNotifications }
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        HomeScreen(
                            onMangaClick = { manga ->
                                val encUrl = URLEncoder.encode(manga.url, "UTF-8")
                                val encTitle = URLEncoder.encode(manga.title, "UTF-8")
                                val encSource = URLEncoder.encode(manga.sourceName, "UTF-8")
                                rootNavController.navigate("manga_details/$encSource?mangaUrl=$encUrl&mangaTitle=$encTitle&resume=false")
                            },
                            onResumeClick = { manga ->
                                var directJumpSuccess = false
                                try {
                                    val hash = manga.url.hashCode()
                                    val capsFile = File(context.cacheDir, "${hash}_caps.json")
                                    if (capsFile.exists()) {
                                        val cArray = JSONArray(capsFile.readText())
                                        val cachedCaps = mutableListOf<ChapterInfo>()
                                        for (i in 0 until cArray.length()) {
                                            val cObj = cArray.getJSONObject(i)
                                            cachedCaps.add(ChapterInfo(name = cObj.getString("name"), url = cObj.getString("url")))
                                        }
                                        val progress = LibraryManager.progressMap.value[manga.url]
                                        val lastReadIndex = cachedCaps.indexOfFirst { it.url == progress?.lastChapterUrl }
                                        val chapterToOpen = if (lastReadIndex >= 0) {
                                            cachedCaps[lastReadIndex]
                                        } else {
                                            val numRegex = Regex("\\d+(\\.\\d+)?")
                                            val firstNum = numRegex.find(cachedCaps.first().name)?.value?.toDoubleOrNull() ?: 0.0
                                            val lastNum = numRegex.find(cachedCaps.last().name)?.value?.toDoubleOrNull() ?: 0.0
                                            val isDescending = firstNum > lastNum
                                            if (isDescending) cachedCaps.lastOrNull() else cachedCaps.firstOrNull()
                                        }
                                        if (chapterToOpen != null) {
                                            ReaderDataCache.currentChapter = chapterToOpen
                                            ReaderDataCache.chapters = cachedCaps
                                            ReaderDataCache.mangaUrl = manga.url
                                            val safeSource = if (manga.sourceName.isNotBlank()) manga.sourceName else "FuenteDesconocida"
                                            val intent = Intent(context, ReaderActivity::class.java).apply { putExtra("sourceName", safeSource) }
                                            context.startActivity(intent)
                                            directJumpSuccess = true
                                        }
                                    }
                                } catch (e: Exception) { Log.e("SHIORI_APP", "Falló el salto rápido: ${e.message}") }

                                if (!directJumpSuccess) {
                                    val encUrl = URLEncoder.encode(manga.url, "UTF-8")
                                    val encTitle = URLEncoder.encode(manga.title, "UTF-8")
                                    val encSource = URLEncoder.encode(manga.sourceName, "UTF-8")
                                    rootNavController.navigate("manga_details/$encSource?mangaUrl=$encUrl&mangaTitle=$encTitle&resume=true")
                                }
                            }
                        )
                    }
                }
            }
            composable(Routes.EXPLORE) {
                Column(modifier = Modifier.fillMaxSize()) {
                    MainTopAppBar(
                        currentRoute = Routes.EXPLORE,
                        onSearchClick = { rootNavController.navigate(Routes.SEARCH) },
                        onNotificationsClick = { showNotifications = !showNotifications }
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        ExploreScreen(onMangaClick = { url, source, title ->
                            val encUrl = URLEncoder.encode(url, "UTF-8")
                            val encTitle = URLEncoder.encode(title, "UTF-8")
                            val encSource = URLEncoder.encode(source, "UTF-8")
                            rootNavController.navigate("manga_details/$encSource?mangaUrl=$encUrl&mangaTitle=$encTitle&resume=false")
                        })
                    }
                }
            }
            composable(Routes.MENSAJERIA) {
                Column(modifier = Modifier.fillMaxSize()) {
                    MainTopAppBar(
                        currentRoute = Routes.MENSAJERIA,
                        onSearchClick = { rootNavController.navigate(Routes.SEARCH) },
                        onNotificationsClick = { showNotifications = !showNotifications }
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        MensajeriaScreen()
                    }
                }
            }
            composable(Routes.MAS) {
                MoreScreen(
                    onNavigateToExtension = { rootNavController.navigate(Routes.EXTENSION) },
                    onNavigateToMigration = { rootNavController.navigate(Routes.MIGRATE) },
                    onNavigateToStorage = { rootNavController.navigate(Routes.STORAGE_SETTINGS) }
                )
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
            Icon(
                imageVector = Icons.Default.NotificationsOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("No tienes ninguna notificación.", fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }

}

private fun safeUrlDecode(encoded: String): String {
    return try {
        URLDecoder.decode(encoded, "UTF-8")
    } catch (e: Exception) {
        // Si falla el decode, devolvemos el string tal cual sin crashear
        android.util.Log.w("SHIORI_NAV", "URLDecode falló para: $encoded — usando raw")
        encoded
    }
}