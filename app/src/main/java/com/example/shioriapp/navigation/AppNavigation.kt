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
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.isSystemInDarkTheme
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.*
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.geometry.Offset
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
    import androidx.compose.ui.input.nestedscroll.NestedScrollSource
    import androidx.compose.ui.input.nestedscroll.nestedScroll
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.platform.LocalView
    import androidx.compose.ui.res.painterResource
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import androidx.lifecycle.viewmodel.compose.viewModel
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
    import com.example.shioriapp.viewmodel.AuthViewModel
    import com.example.shioriapp.screens.*
    import com.example.shioriapp.viewmodel.ExploreViewModel
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
        const val LOGIN = "login"
        const val SETTINGS = "settings_screen"
        const val REPOSITORY = "repository_screen"
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
        val authViewModel: AuthViewModel =
            viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(context.applicationContext) as T
                }
            })
        val authState by authViewModel.authState.collectAsState()

        NavHost(
            navController = rootNavController,
            startDestination = "auth_wrapper",
            modifier = Modifier.fillMaxSize(),
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(300)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            composable("auth_wrapper") {
                MainTabsScreen(
                    rootNavController = rootNavController,
                    authViewModel = authViewModel
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = {
                        rootNavController.popBackStack()
                    }
                )
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

            composable(Routes.REPOSITORY){
                RepositoryScreen(onBack = { rootNavController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { rootNavController.popBackStack() }
                )
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

            composable(
                route = "chat_room/{chatId}",
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                val currentAuthState by authViewModel.authState.collectAsState()
                val currentUserId = (currentAuthState as? AuthViewModel.AuthState.Authenticated)?.userId ?: ""

                ChatRoomScreen(
                    chatId = chatId,
                    currentUserId = currentUserId,
                    navController = rootNavController
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainTopAppBar(
        currentRoute: String,
        isCollapsed: Boolean,
        onSearchClick: () -> Unit,
        onNotificationsClick: () -> Unit,
        onAccountClick: () -> Unit,
        onAddFriendClick: () -> Unit = {},
        showAddFriend: Boolean = false,
        modifier: Modifier = Modifier
    ) {
        val isDarkTheme = isSystemInDarkTheme()

        val pillColor by animateColorAsState(
            targetValue = if (isCollapsed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f) else Color.Transparent,
            animationSpec = tween(300),
            label = "pillColor"
        )

        CenterAlignedTopAppBar(
            modifier = modifier,
            title = {
                AnimatedVisibility(
                    visible = !isCollapsed,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300))
                ) {
                    when (currentRoute) {
                        Routes.HOME -> {
                            val logoRes = if (isDarkTheme) R.drawable.ic_shiori_black else R.drawable.ic_shiori_white
                            Image(
                                painter = painterResource(id = logoRes),
                                contentDescription = null,
                                modifier = Modifier.height(28.dp)
                            )
                        }
                        Routes.EXPLORE -> Text("Explorar", fontWeight = FontWeight.Bold)
                        Routes.MENSAJERIA -> Text(if (showAddFriend) "Buscar Amigos" else "Mensajería", fontWeight = FontWeight.Bold)
                        else -> Text("Shiori", fontWeight = FontWeight.Bold)
                    }
                }
            },
            navigationIcon = {
                Surface(
                    shape = CircleShape,
                    color = pillColor,
                    modifier = Modifier.padding(start = if (isCollapsed) 8.dp else 0.dp)
                ) {
                    IconButton(onClick = onAccountClick) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Cuenta")
                    }
                }
            },
            actions = {
                Surface(
                    shape = CircleShape,
                    color = pillColor,
                    modifier = Modifier.padding(end = if (isCollapsed) 8.dp else 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = if (isCollapsed) 4.dp else 0.dp)
                    ) {
                        if (currentRoute == Routes.MENSAJERIA) {
                            IconButton(onClick = onAddFriendClick) {
                                Icon(
                                    imageVector = if (showAddFriend) Icons.Default.Close else Icons.Default.PersonAdd,
                                    contentDescription = "Amigos"
                                )
                            }
                        }
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Default.Search, "Buscar")
                        }
                        IconButton(onClick = onNotificationsClick) {
                            Icon(Icons.Default.Notifications, "Notificaciones")
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainTabsScreen(
        rootNavController: NavHostController,
        authViewModel: AuthViewModel
    ) {
        val tabsNavController = rememberNavController()
        val navBackStackEntry by tabsNavController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: Routes.HOME
        var showNotifications by remember { mutableStateOf(false) }
        var showAddFriend by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val exploreViewModel: ExploreViewModel = viewModel()
        val currentAuthState by authViewModel.authState.collectAsState()
        var showLoginDialog by remember { mutableStateOf(false) }

        var scrollOffset by remember { mutableFloatStateOf(0f) }
        var isCollapsed by remember { mutableStateOf(false) }

        // === NUEVA VARIABLE PARA LA PASTILLA ===
        var hideBottomBar by remember { mutableStateOf(false) }

        LaunchedEffect(currentRoute) {
            scrollOffset = 0f
            isCollapsed = false
            hideBottomBar = false // Reseteamos al cambiar de pestaña
        }

        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    scrollOffset -= consumed.y

                    if (available.y > 0f && consumed.y == 0f) {
                        scrollOffset = 0f
                    }

                    scrollOffset = scrollOffset.coerceAtLeast(0f)

                    if (scrollOffset <= 5f) {
                        isCollapsed = false
                    } else if (scrollOffset > 40f) {
                        isCollapsed = true
                    }

                    if (available.y < -0.1f) {
                        hideBottomBar = true
                    }

                    if (consumed.y > 0f || available.y > 0f) {
                        hideBottomBar = false
                    }

                    return Offset.Zero
                }
            }
        }

        val view = LocalView.current
        val isDarkTheme = isSystemInDarkTheme()

        LaunchedEffect(isDarkTheme) {
            val window = (view.context as? android.app.Activity)?.window ?: return@LaunchedEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .background(MaterialTheme.colorScheme.background)
        ) {
            NavHost(
                navController = tabsNavController,
                startDestination = Routes.HOME,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(200)) }
            ) {
                composable(Routes.HOME) {
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
                                        cachedCaps.add(
                                            ChapterInfo(
                                                name = cObj.getString("name"),
                                                url = cObj.getString("url")
                                            )
                                        )
                                    }
                                    val progress = LibraryManager.progressMap.value[manga.url]
                                    val lastReadIndex =
                                        cachedCaps.indexOfFirst { it.url == progress?.lastChapterUrl }
                                    val chapterToOpen = if (lastReadIndex >= 0) {
                                        cachedCaps[lastReadIndex]
                                    } else {
                                        val numRegex = Regex("\\d+(\\.\\d+)?")
                                        val firstNum =
                                            numRegex.find(cachedCaps.first().name)?.value?.toDoubleOrNull()
                                                ?: 0.0
                                        val lastNum =
                                            numRegex.find(cachedCaps.last().name)?.value?.toDoubleOrNull()
                                                ?: 0.0
                                        val isDescending = firstNum > lastNum
                                        if (isDescending) cachedCaps.lastOrNull() else cachedCaps.firstOrNull()
                                    }
                                    if (chapterToOpen != null) {
                                        ReaderDataCache.currentChapter = chapterToOpen
                                        ReaderDataCache.chapters = cachedCaps
                                        ReaderDataCache.mangaUrl = manga.url
                                        val safeSource =
                                            if (manga.sourceName.isNotBlank()) manga.sourceName else "FuenteDesconocida"
                                        val intent = Intent(
                                            context,
                                            ReaderActivity::class.java
                                        ).apply { putExtra("sourceName", safeSource) }
                                        context.startActivity(intent)
                                        directJumpSuccess = true
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("SHIORI_APP", "Falló el salto rápido: ${e.message}")
                            }
                            if (!directJumpSuccess) {
                                val encUrl = URLEncoder.encode(manga.url, "UTF-8")
                                val encTitle = URLEncoder.encode(manga.title, "UTF-8")
                                val encSource = URLEncoder.encode(manga.sourceName, "UTF-8")
                                rootNavController.navigate("manga_details/$encSource?mangaUrl=$encUrl&mangaTitle=$encTitle&resume=true")
                            }
                        }
                    )
                }

                composable(Routes.EXPLORE) {
                    ExploreScreen(
                        viewModel = exploreViewModel,
                        onMangaClick = { url, source, title ->
                            val encUrl = URLEncoder.encode(url, "UTF-8")
                            val encTitle = URLEncoder.encode(title, "UTF-8")
                            val encSource = URLEncoder.encode(source, "UTF-8")
                            rootNavController.navigate("manga_details/$encSource?mangaUrl=$encUrl&mangaTitle=$encTitle&resume=false")
                        }
                    )
                }
                composable(Routes.MENSAJERIA) {
                    val currentAuthState by authViewModel.authState.collectAsState()
                    val currentUserId =
                        (currentAuthState as? AuthViewModel.AuthState.Authenticated)?.userId

                    MensajeriaScreen(
                        userId = currentUserId,
                        navController = rootNavController
                    )
                }

                composable(Routes.MAS) {
                    MoreScreen(
                        authViewModel = authViewModel,
                        onNavigateToExtension = { rootNavController.navigate(Routes.EXTENSION) },
                        onNavigateToMigration = { rootNavController.navigate(Routes.MIGRATE) },
                        onNavigateToStorage = { rootNavController.navigate(Routes.STORAGE_SETTINGS) },
                        onNavigateToRepository = {rootNavController.navigate(Routes.REPOSITORY) },
                        onNavigateToSettings = { rootNavController.navigate(Routes.SETTINGS) }
                    )
                }
            }

            if (currentRoute != Routes.MAS) {
                MainTopAppBar(
                    currentRoute = currentRoute,
                    isCollapsed = isCollapsed,
                    onSearchClick = { rootNavController.navigate(Routes.SEARCH) },
                    onNotificationsClick = { showNotifications = !showNotifications },
                    onAccountClick = {
                        if (currentAuthState is AuthViewModel.AuthState.Authenticated) {
                            tabsNavController.navigate(Routes.MAS)
                        } else {
                            showLoginDialog = true
                        }
                    },
                    onAddFriendClick = { showAddFriend = !showAddFriend },
                    showAddFriend = showAddFriend,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                )
            }

            // === PASTILLA CON VARIABLE INDEPENDIENTE ===
            AnimatedVisibility(
                visible = !hideBottomBar,
                enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(300)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    color = if (isDarkTheme) Color(0xFF252527) else MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val items = listOf(
                            Triple(Routes.HOME, Icons.Default.Home, "Biblioteca"),
                            Triple(Routes.EXPLORE, Icons.Default.Explore, "Explorar"),
                            Triple(Routes.MENSAJERIA, Icons.Default.ChatBubbleOutline, "Mensajes"),
                            Triple(Routes.MAS, Icons.Default.Menu, "Menú")
                        )

                        items.forEach { (route, icon, label) ->
                            val isSelected = currentRoute == route

                            val backgroundColor by animateColorAsState(
                                targetValue = if (isSelected) {
                                    if (isDarkTheme) Color(0xFF4A4A4F) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                } else Color.Transparent,
                                animationSpec = tween(300),
                                label = "bg_color"
                            )

                            val contentColor = if (isSelected) {
                                if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
                            } else {
                                if (isDarkTheme) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(backgroundColor)
                                    .clickable {
                                        if (currentRoute != route) {
                                            tabsNavController.navigate(route) {
                                                popUpTo(Routes.HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        } else if (route == Routes.EXPLORE) {
                                            exploreViewModel.toggleSourcesView(true)
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = contentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showNotifications) {
                NotificationDropdown(expanded = showNotifications, onDismiss = { showNotifications = false })
            }

            if (showLoginDialog) {
                AlertDialog(
                    onDismissRequest = { showLoginDialog = false },
                    title = {
                        Text("No has iniciado sesión", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text("Inicia sesión o crea una cuenta para acceder a tu perfil, guardar tu progreso en la nube y chatear con amigos.")
                    },
                    icon = {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(48.dp))
                    },
                    confirmButton = {
                        Button(onClick = {
                            showLoginDialog = false
                            rootNavController.navigate(Routes.LOGIN)
                        }) {
                            Text("Iniciar sesión")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLoginDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
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
            android.util.Log.w("SHIORI_NAV", "URLDecode falló para: $encoded — usando raw")
            encoded
        }
    }