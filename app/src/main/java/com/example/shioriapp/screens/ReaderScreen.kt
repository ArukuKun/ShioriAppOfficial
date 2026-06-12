package com.example.shioriapp.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.example.shioriapp.navigation.ReaderDataCache
import com.example.shioriapp.viewmodel.ReaderViewModel
import com.example.shioriapp.data.repository.LibraryManager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.foundation.shape.CircleShape

@Composable
fun ReaderScreen(
    sourceName: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showOverlay by remember { mutableStateOf(false) }

    var showSpotifySheet by remember { mutableStateOf(false) }
    @OptIn(ExperimentalMaterial3Api::class)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val listState = rememberLazyListState()
    var hasRestoredInitialPosition by remember { mutableStateOf(false) }
    var isReadyToSaveProgress by remember { mutableStateOf(false) }

    val activity = context as? Activity
    val window = activity?.window
    val controller = remember(window) { window?.let { WindowCompat.getInsetsController(it, it.decorView) } }

    var anchorKey by remember { mutableStateOf<String?>(null) }
    var anchorOffset by remember { mutableIntStateOf(0) }
    var isPrepending by remember { mutableStateOf(false) }

    val imageLoader = context.imageLoader
    val currentFirstVisible by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    // CONFIGURACIÓN INMERSIVA INICIAL
    DisposableEffect(Unit) {
        window?.let {
            it.statusBarColor = android.graphics.Color.TRANSPARENT
            it.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        controller?.let {
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            it.isAppearanceLightStatusBars = false
            it.isAppearanceLightNavigationBars = false

            it.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            viewModel.clearReader()
            ReaderDataCache.currentChapter = null
            ReaderDataCache.chapters = emptyList()
            ReaderDataCache.mangaUrl = ""
        }
    }

    // ANIMAR BARRAS AL TOCAR LA PANTALLA
    LaunchedEffect(showOverlay) {
        controller?.let {
            if (showOverlay) {
                it.show(WindowInsetsCompat.Type.systemBars())
            } else {
                it.hide(WindowInsetsCompat.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    val safeExit = {
        controller?.show(WindowInsetsCompat.Type.systemBars())
        onBack()
    }

    BackHandler { safeExit() }

    LaunchedEffect(Unit) {
        val chapter = ReaderDataCache.currentChapter
        val chapters = ReaderDataCache.chapters
        if (chapter != null) {
            viewModel.initReader(chapter, chapters, sourceName)
        }
    }

    LaunchedEffect(currentFirstVisible, state.pages) {
        if (state.pages.isEmpty() || !hasRestoredInitialPosition) return@LaunchedEffect

        val start = (currentFirstVisible - 5).coerceAtLeast(0)
        val end = (currentFirstVisible + 5).coerceAtMost(state.pages.lastIndex)

        for (i in start..end) {
            val pageUrl = state.pages[i].page.imageUrl ?: state.pages[i].page.url
            if (pageUrl.isNotBlank()) {
                val request = ImageRequest.Builder(context)
                    .data(pageUrl)
                    .memoryCacheKey(pageUrl)
                    .diskCacheKey(pageUrl)
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    LaunchedEffect(state.pages) {
        if (isPrepending && anchorKey != null) {
            val newIndex = state.pages.indexOfFirst { it.uniqueId == anchorKey }
            if (newIndex >= 0) {
                listState.scrollToItem(newIndex, anchorOffset)
            }
            isPrepending = false
            anchorKey = null

        } else if (state.pages.isNotEmpty() && !hasRestoredInitialPosition) {
            val chapter = ReaderDataCache.currentChapter
            val mangaUrl = ReaderDataCache.mangaUrl
            val progress = LibraryManager.progressMap.value[mangaUrl]

            if (chapter != null && progress != null && progress.lastChapterUrl == chapter.url && progress.lastPage > 0) {
                val targetIndex = state.pages.indexOfFirst {
                    it.chapter.url == chapter.url && it.displayIndex == progress.lastPage
                }
                if (targetIndex >= 0) {
                    listState.scrollToItem(targetIndex)
                }
            }
            hasRestoredInitialPosition = true
            kotlinx.coroutines.delay(2000)
            isReadyToSaveProgress = true
        }
    }

    LaunchedEffect(currentFirstVisible, isReadyToSaveProgress) {
        if (isReadyToSaveProgress && state.pages.isNotEmpty() && currentFirstVisible < state.pages.size) {
            val currentPage = state.pages[currentFirstVisible]
            val mangaUrl = ReaderDataCache.mangaUrl
            val isFinished = currentPage.displayIndex >= currentPage.totalPages

            LibraryManager.saveProgress(
                context = context,
                mangaUrl = mangaUrl,
                chapterUrl = currentPage.chapter.url,
                page = currentPage.displayIndex,
                totalPages = currentPage.totalPages,
                isFinished = isFinished
            )
        }
    }

    val shouldLoadPrevious by remember {
        derivedStateOf {
            hasRestoredInitialPosition && state.pages.isNotEmpty() && currentFirstVisible <= 3
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadPrevious) {
        if (shouldLoadPrevious && !state.isLoadingPrev) {
            val firstItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            anchorKey = firstItem?.key as? String
            anchorOffset = firstItem?.offset ?: 0
            isPrepending = true
            viewModel.loadPrev()
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isLoadingNext) {
            viewModel.loadNext()
        }
    }

    val currentVisibleChapterName by remember {
        derivedStateOf {
            if (!hasRestoredInitialPosition) return@derivedStateOf ReaderDataCache.currentChapter?.name ?: "Lector"
            val firstVisibleIndex = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
            if (state.pages.isNotEmpty() && firstVisibleIndex < state.pages.size) {
                state.pages[firstVisibleIndex].chapter.name
            } else {
                ReaderDataCache.currentChapter?.name ?: "Lector"
            }
        }
    }

    val currentVisiblePageInfo by remember {
        derivedStateOf {
            if (!hasRestoredInitialPosition) return@derivedStateOf null
            val firstVisibleIndex = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
            if (state.pages.isNotEmpty() && firstVisibleIndex < state.pages.size) {
                state.pages[firstVisibleIndex]
            } else null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (hasRestoredInitialPosition) 1f else 0f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showOverlay = !showOverlay }
        ) {
            if (state.isLoadingPrev) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }

            itemsIndexed(state.pages, key = { _, page -> page.uniqueId }) { index, readerPage ->

                if (index > 0 && state.pages[index - 1].chapter.url != readerPage.chapter.url) {
                    ChapterTransitionDivider(
                        prevName = cleanChapterName(state.pages[index - 1].chapter.name),
                        nextName = cleanChapterName(readerPage.chapter.name)
                    )
                }

                val imageUrl = readerPage.page.imageUrl ?: readerPage.page.url

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(Color.Black)
                        .animateContentSize()
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Página ${readerPage.displayIndex}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth(),
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp)
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.DarkGray)
                            }
                        }
                    )
                }
            }

            if (state.isLoadingNext) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }

        if (state.isLoadingInitial || (state.pages.isNotEmpty() && !hasRestoredInitialPosition)) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
        }

        if (currentVisiblePageInfo != null && hasRestoredInitialPosition) {
            val pageInfo = currentVisiblePageInfo!!

            val (capActual, capTotal) = remember(pageInfo.chapter.url) {
                val total = ReaderDataCache.chapters.size
                val index = ReaderDataCache.chapters.indexOfFirst { it.url == pageInfo.chapter.url }
                val current = if (index != -1) total - index else 0
                Pair(current, total)
            }

            AnimatedVisibility(
                visible = showOverlay,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF151515).copy(alpha = 0.95f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cap. $capActual/$capTotal  •  Pág. ${pageInfo.displayIndex}/${pageInfo.totalPages}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showOverlay && hasRestoredInitialPosition,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF151515))
                    .statusBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp).fillMaxWidth()
                ) {
                    IconButton(onClick = { safeExit() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    Text(
                        text = cleanChapterName(currentVisibleChapterName),
                        color = Color.White,
                        maxLines = 1,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = { showSpotifySheet = true }) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Spotify",
                            tint = Color(0xFF1DB954)
                        )
                    }

                    @OptIn(ExperimentalMaterial3Api::class)
                    if (showSpotifySheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showSpotifySheet = false },
                            sheetState = sheetState,
                            containerColor = Color(0xFF121212), // Fondo oscuro premium
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Reproduciendo OST",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Portada del Álbum (Diseño temporal)
                                Box(
                                    modifier = Modifier
                                        .size(200.dp)
                                        .background(Color(0xFF282828), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Text("Gurenge (Demon Slayer)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, maxLines = 1)
                                Text("LiSA", color = Color(0xFFB3B3B3), fontSize = 16.sp)

                                Spacer(modifier = Modifier.height(32.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(0.8f),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { /* TODO: Canción Anterior */ }) {
                                        Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(42.dp))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(Color(0xFF1DB954), CircleShape)
                                            .clickable { /* TODO: Play/Pausa */ },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(42.dp))
                                    }

                                    IconButton(onClick = { /* TODO: Canción Siguiente */ }) {
                                        Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = Color.White, modifier = Modifier.size(42.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterTransitionDivider(prevName: String, nextName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF121212))
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Fin de", color = Color.Gray, fontSize = 12.sp)
        Text(prevName, color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.width(100.dp), thickness = 2.dp)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Siguiente", color = Color.White, fontSize = 12.sp)
        Text(nextName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
    }
}

fun cleanChapterName(rawName: String): String {
    val regex = Regex("(?i)(capítulo|capitulo|chapter|cap\\.?|ch\\.?)\\s*\\d+(\\.\\d+)?")
    val match = regex.find(rawName)

    if (match != null) {
        return match.value.replaceFirstChar { it.uppercase() }
    }

    return rawName.split("-", ":").first().trim()
}

