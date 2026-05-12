package com.example.shioriapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.shioriapp.viewmodel.ExtensionViewModel
import com.example.shioriapp.viewmodel.InstallState

// Enum para manejar el estado visual de las extensiones +18
enum class NsfwFilterState { SHOW_ALL, ONLY_NSFW, HIDE_NSFW }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    onBack: () -> Unit,
    viewModel: ExtensionViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val extensions by viewModel.extensions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val installStates by viewModel.installStates.collectAsState()

    var filterQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Todas", "Instaladas", "Actualizaciones")

    // --- NUEVOS ESTADOS PARA FILTROS ---
    var showFilterSheet by remember { mutableStateOf(false) }
    var nsfwFilter by remember { mutableStateOf(NsfwFilterState.SHOW_ALL) }
    var selectedLanguage by remember { mutableStateOf("Todos") }

    // Generamos la lista de idiomas disponibles automáticamente leyendo del JSON
    val availableLanguages = remember(extensions) {
        listOf("Todos") + extensions.map { it.lang.uppercase() }.distinct().sorted()
    }

    // --- LÓGICA DE FILTRADO COMBINADA ---
    val filteredExtensions = remember(filterQuery, extensions, selectedLanguage, nsfwFilter) {
        extensions.filter {
            val nameWithoutTachiyomi = it.name.replace("Tachiyomi: ", "").trim()
            val matchesSearch = nameWithoutTachiyomi.contains(filterQuery, ignoreCase = true) ||
                    it.pkg.contains(filterQuery, ignoreCase = true)

            // Filtro por idioma
            val matchesLang = selectedLanguage == "Todos" || it.lang.uppercase() == selectedLanguage

            // Filtro por contenido explícito
            val matchesNsfw = when (nsfwFilter) {
                NsfwFilterState.SHOW_ALL -> true
                NsfwFilterState.ONLY_NSFW -> it.nsfw == 1
                NsfwFilterState.HIDE_NSFW -> it.nsfw == 0
            }

            matchesSearch && matchesLang && matchesNsfw
        }
    }

    val uninstalledExtensions = filteredExtensions.filter {
        val s = installStates[it.pkg]
        s == InstallState.IDLE || s == InstallState.ERROR || s == InstallState.DOWNLOADING_NEW || s == null
    }

    val installedExtensions = filteredExtensions.filter {
        val s = installStates[it.pkg]
        s == InstallState.INSTALLED || s == InstallState.UPDATABLE || s == InstallState.DOWNLOADING_UPDATE
    }

    val updatableExtensions = filteredExtensions.filter {
        val s = installStates[it.pkg]
        s == InstallState.UPDATABLE || s == InstallState.DOWNLOADING_UPDATE
    }

    val extensionsToShow = when (selectedTabIndex) {
        0 -> uninstalledExtensions
        1 -> installedExtensions
        2 -> updatableExtensions
        else -> emptyList()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.registerPackageReceiver(context)
                viewModel.refreshStates(context)
            } else if (event == Lifecycle.Event.ON_STOP) {
                viewModel.unregisterPackageReceiver(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.unregisterPackageReceiver(context)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadExtensions(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Extensiones",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filtros",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar por nombre o paquete...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            val tabText = if (index == 2 && updatableExtensions.isNotEmpty()) {
                                "$title (${updatableExtensions.size})"
                            } else {
                                title
                            }
                            Text(
                                text = tabText,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    item {
                        Text(
                            text = "${extensionsToShow.size} extensiones",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                    items(extensionsToShow) { extension ->
                        val currentState = installStates[extension.pkg] ?: InstallState.IDLE
                        val displayName = extension.name.replace("Tachiyomi: ", "").trim()

                        RepositoryExtensionCard(
                            name = displayName,
                            pkg = extension.pkg,
                            version = extension.version,
                            lang = extension.lang,
                            nsfw = extension.nsfw == 1,
                            state = currentState,
                            selectedTab = selectedTabIndex,
                            onInstallClick = { viewModel.installExtension(context, extension) },
                            onUninstallClick = { viewModel.uninstallExtension(context, extension) }
                        )
                    }
                }
            }
        }

        // --- NUEVO: BOTTOM SHEET DE FILTROS DESLIZABLE ---
        if (showFilterSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Filtros de Búsqueda",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = "Contenido Explícito",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = nsfwFilter == NsfwFilterState.SHOW_ALL,
                            onClick = { nsfwFilter = NsfwFilterState.SHOW_ALL },
                            label = { Text("Todo") }
                        )
                        FilterChip(
                            selected = nsfwFilter == NsfwFilterState.HIDE_NSFW,
                            onClick = { nsfwFilter = NsfwFilterState.HIDE_NSFW },
                            label = { Text("Ocultar +18") }
                        )
                        FilterChip(
                            selected = nsfwFilter == NsfwFilterState.ONLY_NSFW,
                            onClick = { nsfwFilter = NsfwFilterState.ONLY_NSFW },
                            label = { Text("Solo +18") }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Idioma",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        items(availableLanguages) { lang ->
                            FilterChip(
                                selected = selectedLanguage == lang,
                                onClick = { selectedLanguage = lang },
                                label = { Text(lang) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RepositoryExtensionCard(
    name: String,
    pkg: String,
    version: String,
    lang: String,
    nsfw: Boolean,
    state: InstallState,
    selectedTab: Int,
    onInstallClick: () -> Unit,
    onUninstallClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/icon/${pkg}.png"
            AsyncImage(
                model = iconUrl,
                contentDescription = "Icono de $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (nsfw) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = "18+",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = pkg,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "v$version • ${lang.uppercase()}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            when (selectedTab) {
                0 -> { // Pestaña: Todas
                    val isDownloading = state == InstallState.DOWNLOADING_NEW
                    Button(
                        onClick = onInstallClick,
                        enabled = !isDownloading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (isDownloading) "Descargando..." else "Instalar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                1 -> { // Pestaña: Instaladas
                    val isDownloading = state == InstallState.DOWNLOADING_UPDATE
                    if (isDownloading) {
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        ) {
                            Text("Descargando...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onUninstallClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Desinstalar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                2 -> { // Pestaña: Actualizaciones
                    val isDownloading = state == InstallState.DOWNLOADING_UPDATE
                    Button(
                        onClick = onInstallClick,
                        enabled = !isDownloading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800) // Naranja
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (isDownloading) "Descargando..." else "Actualizar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}