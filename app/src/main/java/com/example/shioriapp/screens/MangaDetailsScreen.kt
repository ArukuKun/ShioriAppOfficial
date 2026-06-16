package com.example.shioriapp.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.shioriapp.ui.theme.NeutralGris
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.shioriapp.R
import com.example.shioriapp.data.repository.LibraryManager
import com.example.shioriapp.domain.model.ChapterInfo
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.viewmodel.MangaDetailViewModel
import com.example.shioriapp.core.util.MangaDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailsScreen(
    mangaUrl: String,
    sourceName: String,
    mangaTitle: String,
    autoResume: Boolean,
    onBack: () -> Unit,
    onChapterClick: (ChapterInfo, List<ChapterInfo>) -> Unit,
    onCategoryClick: () -> Unit,
    onMigrateClick: (MangaInfo) -> Unit,
    onDownloadChapter: (ChapterInfo) -> Unit,
    onDownloadAll: (List<ChapterInfo>) -> Unit,
    onDeleteChapter: (ChapterInfo) -> Unit,
    viewModel: MangaDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val localContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val sortDescending by LibraryManager.isChapterSortDescending.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var isSynopsisExpanded by remember { mutableStateOf(false) }
    val downloadStates = remember { mutableStateMapOf<String, DownloadState>() }

    LaunchedEffect(mangaUrl) {
        LibraryManager.init(localContext)
        viewModel.loadMangaDetails(localContext, mangaUrl, sourceName, mangaTitle)
    }

    val prefs = localContext.getSharedPreferences("shiori_storage", Context.MODE_PRIVATE)
    val downloadPath = prefs.getString("download_path", "") ?: ""

    LaunchedEffect(state.chapters, downloadPath) {
        if (downloadPath.startsWith("content://") && state.chapters.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                state.chapters.forEach { chapter ->
                    val exists = MangaDownloader.isChapterDownloaded(localContext, downloadPath, mangaTitle, chapter.name)
                    withContext(Dispatchers.Main) {
                        downloadStates[chapter.url] = if (exists) DownloadState.Downloaded else DownloadState.None
                    }
                }
            }
        }
    }

    val library by LibraryManager.library.collectAsState()
    val isFavorite = state.manga?.let { manga ->
        library.any { it.url == manga.url && it.sourceName == sourceName }
    } ?: false

    val progressMap by LibraryManager.progressMap.collectAsState()
    val mangaProgress = progressMap[mangaUrl]

    val scrollState = rememberScrollState()
    val isAtBottom by remember {
        derivedStateOf {
            scrollState.maxValue > 0 && scrollState.value >= (scrollState.maxValue - 50)
        }
    }

    val sortedChapters = remember(state.chapters, sortDescending) {
        if (sortDescending) state.chapters else state.chapters.reversed()
    }
    val chaptersAsc = remember(state.chapters) { state.chapters.reversed() }
    val readChapters = mangaProgress?.readChapters ?: emptySet()
    val hasProgress = mangaProgress?.lastChapterUrl?.isNotBlank() == true
    val allRead = hasProgress && chaptersAsc.isNotEmpty() && chaptersAsc.all { it.url in readChapters }

    val chapterToOpen: ChapterInfo? = when {
        chaptersAsc.isEmpty() -> null
        hasProgress -> {
            val lastReadIndex = chaptersAsc.indexOfFirst { it.url == mangaProgress?.lastChapterUrl }
            val lastChapterFinished = mangaProgress?.lastChapterUrl in readChapters
            when {
                lastReadIndex >= 0 && lastReadIndex < chaptersAsc.lastIndex && lastChapterFinished -> chaptersAsc[lastReadIndex + 1]
                else -> chaptersAsc[lastReadIndex.coerceAtLeast(0)]
            }
        }
        else -> chaptersAsc.first()
    }

    var hasAutoResumed by rememberSaveable { mutableStateOf(false) }
    var hasStartedLoading by remember { mutableStateOf(false) }
    val isCurrentlyAutoResuming = autoResume && !hasAutoResumed

    LaunchedEffect(state.isLoading) {
        if (state.isLoading) {
            hasStartedLoading = true
        } else if (hasStartedLoading) {
            if (isCurrentlyAutoResuming) {
                if (state.chapters.isNotEmpty() && chapterToOpen != null) {
                    hasAutoResumed = true
                    onChapterClick(chapterToOpen, state.chapters)
                } else {
                    hasAutoResumed = true
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = ImageRequest.Builder(localContext).data(state.manga?.coverUrl).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.3f).blur(25.dp)
        )

        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                if (chapterToOpen != null) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isAtBottom,
                        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                if (allRead) {
                                    LibraryManager.unmarkAllChapters(localContext, mangaUrl)
                                    onChapterClick(chaptersAsc.first(), state.chapters)
                                } else {
                                    onChapterClick(chapterToOpen, state.chapters)
                                }
                            },
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            icon = {
                                Icon(imageVector = if (allRead) Icons.Default.Replay else if (hasProgress) Icons.Default.PlayArrow else Icons.Default.Book, contentDescription = null)
                            },
                            text = { Text(if (allRead) "Releer" else if (hasProgress) "Reanudar" else "Comenzar a leer") }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = paddingValues.calculateBottomPadding())
                        .verticalScroll(scrollState)
                ) {
                    Spacer(modifier = Modifier.statusBarsPadding().height(64.dp))

                    // 🔥 LAYOUT RESPONSIVO CON FILAS ADAPTABLES
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        val isWideScreen = maxWidth > 600.dp

                        if (isWideScreen) {
                            // Layout para tablets/pantallas grandes
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(localContext).data(state.manga?.coverUrl)
                                        .crossfade(true).build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .width(180.dp)
                                        .aspectRatio(0.7f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    MangaDetailsContent(state = state, mangaTitle = mangaTitle, sourceName = sourceName, isFavorite = isFavorite, localContext = localContext)
                                }
                            }
                        } else {
                            // Layout para móviles
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = ImageRequest.Builder(localContext).data(state.manga?.coverUrl)
                                        .crossfade(true).build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .width(120.dp)
                                        .aspectRatio(0.7f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    MangaDetailsContent(state = state, mangaTitle = mangaTitle, sourceName = sourceName, isFavorite = isFavorite, localContext = localContext)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ActionIcon(icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, label = if (isFavorite) "En Biblioteca" else "Añadir", onClick = { state.manga?.let { LibraryManager.toggleManga(localContext, it.copy(sourceName = sourceName)) } })
                        ActionIcon(icon = Icons.Default.Share, label = "Compartir", onClick = { /* Lógica compartir */ })
                        ActionIcon(icon = Icons.Default.Sync, label = "Migrar", onClick = {
                            val currentManga = state.manga?.copy(sourceName = sourceName) ?: MangaInfo(title = mangaTitle, url = mangaUrl, sourceName = sourceName, coverUrl = state.manga?.coverUrl ?: "", author = state.manga?.author ?: "", status = state.manga?.status ?: 0, genres = state.manga?.genres ?: "")
                            onMigrateClick(currentManga)
                        })
                        ActionIcon(
                            icon = Icons.Default.FileDownload,
                            label = "Descargar",
                            onClick = {
                                if (state.chapters.isNotEmpty() && downloadPath.startsWith("content://")) {
                                    state.chapters.forEach { chapter ->
                                        if (downloadStates[chapter.url] == null || downloadStates[chapter.url] is DownloadState.None) {
                                            coroutineScope.launch {
                                                for (i in 0..100 step 20) {
                                                    downloadStates[chapter.url] = DownloadState.Downloading(i)
                                                    kotlinx.coroutines.delay(300)
                                                }
                                                downloadStates[chapter.url] = DownloadState.Downloaded
                                            }
                                        }
                                    }
                                }
                                onDownloadAll(state.chapters)
                            }
                        )
                    }

                    var isTextOverflowing by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = isTextOverflowing || isSynopsisExpanded,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { isSynopsisExpanded = !isSynopsisExpanded }
                            .animateContentSize()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Sinopsis",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                        )

                        Text(
                            text = state.manga?.description ?: "No hay descripción disponible.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = TextAlign.Justify,
                            maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { textLayoutResult ->
                                if (!isSynopsisExpanded) {
                                    isTextOverflowing = textLayoutResult.hasVisualOverflow
                                }
                            }
                        )
                        if (isTextOverflowing || isSynopsisExpanded) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSynopsisExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isSynopsisExpanded) "Contraer sinopsis" else "Expandir sinopsis",
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val tags = state.manga?.genres?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                    if (tags.isNotEmpty()) {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tags) { tag ->
                                Surface(color = Color.White.copy(0.1f), shape = RoundedCornerShape(16.dp), modifier = Modifier.clickable { onCategoryClick() }) {
                                    Text(text = tag, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.White.copy(0.1f))

                    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Capítulos (${state.chapters.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        if (state.chapters.isNotEmpty()) {
                            TextButton(onClick = { if (!allRead) LibraryManager.markAllChaptersRead(localContext, mangaUrl, state.chapters.map { it.url }) }) {
                                Icon(imageVector = if (allRead) Icons.Default.DoneAll else Icons.Default.CheckCircle, contentDescription = null, tint = if (allRead) Color(0xFF4CAF50) else Color.White.copy(0.6f), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(text = if (allRead) "Todo leído" else "Marcar todos", color = if (allRead) Color(0xFF4CAF50) else Color.White.copy(0.6f), fontSize = 13.sp)
                            }
                        }
                    }

                    if (state.isLoading && state.chapters.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) { CircularProgressIndicator(color = Color.White) }
                    }

                    sortedChapters.forEach { chapter ->
                        val isCurrentChapter = mangaProgress?.lastChapterUrl == chapter.url
                        val currentDownloadState = downloadStates[chapter.url] ?: DownloadState.None

                        ChapterItem(
                            chapter = chapter,
                            isRead = readChapters.contains(chapter.url),
                            isCurrent = isCurrentChapter,
                            currentPage = if (isCurrentChapter) mangaProgress?.lastPage ?: 0 else 0,
                            totalPages = if (isCurrentChapter) mangaProgress?.totalPages ?: 0 else 0,
                            downloadState = currentDownloadState,
                            onToggleRead = { LibraryManager.toggleChapterRead(localContext, mangaUrl, chapter.url) },
                            onClick = { onChapterClick(chapter, state.chapters) },
                            onDownloadClick = {
                                if (downloadPath.startsWith("content://")) {
                                    coroutineScope.launch {
                                        for (i in 0..100 step 10) {
                                            downloadStates[chapter.url] = DownloadState.Downloading(i)
                                            kotlinx.coroutines.delay(200)
                                        }
                                        downloadStates[chapter.url] = DownloadState.Downloaded
                                    }
                                }
                                onDownloadChapter(chapter)
                            },
                            onDeleteDownload = {
                                downloadStates[chapter.url] = DownloadState.None
                                onDeleteChapter(chapter)
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(Color.Black.copy(0.4f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }

                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(0.4f), RoundedCornerShape(50))
                        ) {
                            Icon(Icons.Default.FilterList, "Ordenar", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Más reciente primero") },
                                leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
                                trailingIcon = {
                                    if (sortDescending) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                },
                                onClick = {
                                    LibraryManager.setChapterSortDescending(localContext, true)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Más antiguo primero") },
                                leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, null) },
                                trailingIcon = {
                                    if (!sortDescending) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                },
                                onClick = {
                                    LibraryManager.setChapterSortDescending(localContext, false)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// 🔥 NUEVA FUNCIÓN AUXILIAR PARA EVITAR REPETICIÓN DE CÓDIGO
@Composable
private fun MangaDetailsContent(
    state: com.example.shioriapp.viewmodel.DetailsState,
    mangaTitle: String,
    sourceName: String,
    isFavorite: Boolean,
    localContext: Context
) {
    Text(
        text = state.manga?.title?.takeIf { it.isNotBlank() } ?: mangaTitle,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    val statusInt = state.manga?.status ?: 0
    val (statusText, statusColor) = when (statusInt) {
        1 -> "En curso" to Color(0xFF4CAF50)
        2, 4 -> "Completado" to Color(0xFFF44336)
        6 -> "Pausado" to Color(0xFFFF9800)
        else -> "Desconocido" to Color.Gray
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            color = statusColor.copy(alpha = 0.2f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(8.dp)) {
                    drawCircle(color = statusColor, radius = 3.dp.toPx())
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    statusText,
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (sourceName.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = sourceName,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChapterItem(
    chapter: ChapterInfo,
    isRead: Boolean,
    isCurrent: Boolean,
    currentPage: Int = 0,
    totalPages: Int = 0,
    downloadState: DownloadState = DownloadState.None,
    onToggleRead: () -> Unit,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onDeleteDownload: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = { showMenu = true })
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .alpha(if (isRead && !isCurrent) 0.4f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCurrent) {
                Box(modifier = Modifier.width(3.dp).height(36.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(10.dp))
            } else { Spacer(Modifier.width(13.dp)) }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.name,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else if (isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
                if (isCurrent) { Text("Leyendo pág. $currentPage", color = MaterialTheme.colorScheme.primary.copy(0.7f), fontSize = 11.sp) }
                else if (isRead) { Text("Leído", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 11.sp) }
            }

            IconButton(
                onClick = { if (downloadState is DownloadState.None) onDownloadClick() },
                modifier = Modifier.size(36.dp),
                enabled = downloadState is DownloadState.None
            ) {
                when (downloadState) {
                    is DownloadState.None -> {
                        Icon(Icons.Default.FileDownload, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                    }
                    is DownloadState.Downloading -> {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { downloadState.progress / 100f },
                                modifier = Modifier.size(26.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(0.1f),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "${downloadState.progress}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    is DownloadState.Downloaded -> {
                        Icon(Icons.Default.DownloadDone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.width(8.dp))
            Icon(imageVector = if (isCurrent) Icons.Default.PlayArrow else if (isRead) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = if (isCurrent) Color.White else if (isRead) Color(0xFF4CAF50) else Color.White.copy(0.25f), modifier = Modifier.size(18.dp))
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text(if (isRead) "Marcar como no leído" else "Marcar como leído") }, leadingIcon = { Icon(Icons.Default.Done, null) }, onClick = { onToggleRead(); showMenu = false })

            if (downloadState is DownloadState.Downloaded) {
                DropdownMenuItem(
                    text = { Text("Eliminar descarga", color = Color.Red) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                    onClick = { onDeleteDownload(); showMenu = false }
                )
            }
        }
    }
}

@Composable
fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

sealed class DownloadState {
    object None : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    object Downloaded : DownloadState()
}