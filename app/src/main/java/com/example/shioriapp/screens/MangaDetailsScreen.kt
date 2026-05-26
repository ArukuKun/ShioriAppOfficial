package com.example.shioriapp.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import com.example.shioriapp.viewmodel.MangaDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailsScreen(
    mangaUrl: String = "",
    sourceName: String = "",
    mangaTitle: String = "Cargando...",
    autoResume: Boolean = false,
    onBack: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onChapterClick: (ChapterInfo, List<ChapterInfo>) -> Unit = { _, _ -> },
    viewModel: MangaDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val localContext = LocalContext.current

    val sortDescending by LibraryManager.isChapterSortDescending.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(mangaUrl) {
        LibraryManager.init(localContext)
        viewModel.loadMangaDetails(localContext, mangaUrl, sourceName, mangaTitle)
    }

    val library by LibraryManager.library.collectAsState()
    val isFavorite = state.manga?.let { manga ->
        library.any { it.url == manga.url && it.sourceName == sourceName }
    } ?: false

    val progressMap by LibraryManager.progressMap.collectAsState()
    val mangaProgress = progressMap[mangaUrl]

    val sortedChapters = remember(state.chapters, sortDescending) {
        if (sortDescending) state.chapters else state.chapters.reversed()
    }

    val chaptersAsc = remember(state.chapters) { state.chapters.reversed() }

    val readChapters = mangaProgress?.readChapters ?: emptySet()
    val hasProgress = mangaProgress?.lastChapterUrl?.isNotBlank() == true
    val allRead = hasProgress && chaptersAsc.isNotEmpty() &&
            chaptersAsc.all { it.url in readChapters }

    val chapterToOpen: ChapterInfo? = when {
        chaptersAsc.isEmpty() -> null
        hasProgress -> {
            val lastReadIndex = chaptersAsc.indexOfFirst { it.url == mangaProgress?.lastChapterUrl }
            val lastChapterFinished = mangaProgress?.lastChapterUrl in readChapters

            when {
                lastReadIndex >= 0 && lastReadIndex < chaptersAsc.lastIndex && lastChapterFinished ->
                    chaptersAsc[lastReadIndex + 1]
                else ->
                    chaptersAsc[lastReadIndex.coerceAtLeast(0)]
            }
        }
        else -> chaptersAsc.first()
    }

    // ── LÓGICA DE AUTO-REANUDAR DEFINITIVA ──────────────────────────────────
    // rememberSaveable sobrevive cuando vas al lector y regresas, arreglando el bucle infinito
    var hasAutoResumed by rememberSaveable { mutableStateOf(false) }
    var hasStartedLoading by remember { mutableStateOf(false) }
    val isCurrentlyAutoResuming = autoResume && !hasAutoResumed

    LaunchedEffect(state.isLoading) {
        if (state.isLoading) {
            hasStartedLoading = true
        } else if (hasStartedLoading) {
            if (isCurrentlyAutoResuming) {
                if (state.chapters.isNotEmpty() && chapterToOpen != null) {
                    hasAutoResumed = true // Marca la acción como consumida
                    onChapterClick(chapterToOpen, state.chapters)
                } else {
                    hasAutoResumed = true // Si hay error, cancela para no quedarse colgado
                }
            }
        }
    }

    // Seguro anti-cuelgues por mala conexión
    LaunchedEffect(isCurrentlyAutoResuming) {
        if (isCurrentlyAutoResuming) {
            kotlinx.coroutines.delay(5000)
            if (!hasAutoResumed) hasAutoResumed = true
        }
    }

    // ────────────────────────────────────────────────────────────────────────

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── CONTENIDO BASE (Solo visible si no estamos reanudando) ──────────
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
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (allRead) {
                                LibraryManager.unmarkAllChapters(localContext, mangaUrl)
                                onChapterClick(chaptersAsc.first(), state.chapters)
                            } else {
                                onChapterClick(chapterToOpen, state.chapters)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.DarkGray,
                        icon = {
                            Icon(
                                imageVector = when {
                                    allRead -> Icons.Default.Replay
                                    hasProgress -> Icons.Default.PlayArrow
                                    else -> Icons.Default.Book
                                },
                                contentDescription = null
                            )
                        },
                        text = {
                            Text(
                                when {
                                    allRead -> "Releer"
                                    hasProgress -> "Reanudar"
                                    else -> "Comenzar a leer"
                                }
                            )
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = paddingValues.calculateBottomPadding())
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.statusBarsPadding().height(64.dp))

                    Row(
                        modifier = Modifier
                            .padding(16.dp).
                            fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(localContext)
                                .data(state.manga?.coverUrl)
                                .crossfade(true)
                                .build(),
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
                            Surface(
                                color = statusColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.
                                    padding(horizontal = 8.dp, vertical = 4.dp),
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
                            Spacer(modifier = Modifier.height(8.dp))
                            val autor = state.manga?.author
                            if (!autor.isNullOrBlank() && autor.lowercase() != "desconocido") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = autor,
                                        color = Color.White.copy(0.7f),
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    AsyncImage(
                                        model = ImageRequest.Builder(localContext)
                                            .data(R.drawable.ic_launcher_foreground)
                                            .crossfade(true).build(),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp).clip(CircleShape)
                                            .background(Color.White.copy(0.1f)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ActionIcon(
                            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            label = if (isFavorite) "En Biblioteca" else "Añadir",
                            onClick = {
                                state.manga?.let { manga ->
                                    LibraryManager.toggleManga(
                                        localContext,
                                        manga.copy(sourceName = sourceName)
                                    )
                                }
                            }
                        )
                        ActionIcon(Icons.Default.Share, "Compartir")
                        ActionIcon(Icons.Default.Sync, "Migrar")
                        ActionIcon(Icons.Default.FileDownload, "Descargar")
                    }

                    Text(
                        "Sinopsis", color = Color.White, fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Text(
                        text = state.manga?.description ?: "No hay descripción disponible.",
                        color = Color.White.copy(0.8f), fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Justify
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val tags = state.manga?.genres?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                    if (tags.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tags) { tag ->
                                Surface(
                                    color = Color.White.copy(0.1f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.clickable { onCategoryClick(tag) }
                                ) {
                                    Text(
                                        text = tag, color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.White.copy(0.1f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Capítulos (${state.chapters.size})",
                            color = Color.White, fontWeight = FontWeight.Bold,
                            fontSize = 18.sp, modifier = Modifier.weight(1f)
                        )
                        if (state.chapters.isNotEmpty()) {
                            TextButton(onClick = {
                                if (!allRead) {
                                    LibraryManager.markAllChaptersRead(localContext, mangaUrl, state.chapters.map { it.url })
                                }
                            }) {
                                Icon(
                                    imageVector = if (allRead) Icons.Default.DoneAll else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (allRead) Color(0xFF4CAF50) else Color.White.copy(0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (allRead) "Todo leído" else "Marcar todos",
                                    color = if (allRead) Color(0xFF4CAF50) else Color.White.copy(0.6f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    if (state.isLoading && state.chapters.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }

                    sortedChapters.forEach { chapter ->
                        val isCurrentChapter = mangaProgress?.lastChapterUrl == chapter.url
                        ChapterItem(
                            chapter = chapter,
                            isRead = readChapters.contains(chapter.url),
                            isCurrent = isCurrentChapter,
                            currentPage = if (isCurrentChapter) mangaProgress?.lastPage ?: 0 else 0,
                            totalPages = if (isCurrentChapter) mangaProgress?.totalPages ?: 0 else 0,
                            onToggleRead = { LibraryManager.toggleChapterRead(localContext, mangaUrl, chapter.url) },
                            onClick = { onChapterClick(chapter, state.chapters) }
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
                            modifier = Modifier.background(Color.Black.copy(0.4f), RoundedCornerShape(50))
                        ) {
                            Icon(Icons.Default.FilterList, "Ordenar", tint = Color.White)
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

        // ── 🔥 CAPA NEGRA ABSOLUTA DE CARGA (Oculta absolutamente todo) ───────
        if (isCurrentlyAutoResuming) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
    onToggleRead: () -> Unit,
    onClick: () -> Unit
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
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(10.dp))
            } else {
                Spacer(Modifier.width(13.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cleanChapterName(chapter.name),
                    color = when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        isRead    -> Color.Gray
                        else      -> Color.White
                    },
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
                when {
                    isCurrent -> {
                        val pageText = when {
                            totalPages > 0  -> "Pág. $currentPage / $totalPages"
                            currentPage > 0 -> "Pág. $currentPage"
                            else            -> ""
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Leyendo",
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (pageText.isNotEmpty()) {
                                Text(
                                    "  ·  $pageText",
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    isRead -> Text("Leído", color = Color.Gray, fontSize = 11.sp)
                }
            }

            Icon(
                imageVector = when {
                    isCurrent -> Icons.Default.PlayArrow
                    isRead    -> Icons.Default.CheckCircle
                    else      -> Icons.Default.RadioButtonUnchecked
                },
                contentDescription = null,
                tint = when {
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isRead    -> Color(0xFF4CAF50)
                    else      -> Color.White.copy(0.25f)
                },
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(if (isRead) "Marcar como no leído" else "Marcar como leído") },
                leadingIcon = { Icon(if (isRead) Icons.Default.RemoveDone else Icons.Default.Done, null) },
                onClick = { onToggleRead(); showMenu = false }
            )
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
        Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}