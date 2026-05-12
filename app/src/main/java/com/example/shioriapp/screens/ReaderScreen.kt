package com.example.shioriapp.screens

import android.app.Activity
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
import coil.request.ImageRequest
import com.example.shioriapp.navigation.ReaderDataCache
import com.example.shioriapp.viewmodel.ReaderViewModel
import com.example.shioriapp.data.repository.LibraryManager // 🔥 Importación necesaria para el progreso

@Composable
fun ReaderScreen(
    sourceName: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showOverlay by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // 1. Arrancamos el lector
    LaunchedEffect(Unit) {
        val chapter = ReaderDataCache.currentChapter
        val chapters = ReaderDataCache.chapters
        if (chapter != null) {
            viewModel.initReader(chapter, chapters, sourceName)
        }
    }

    // 🔥 2. GUARDAR PROGRESO AUTOMÁTICO AL HACER SCROLL
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (state.pages.isNotEmpty() && listState.firstVisibleItemIndex < state.pages.size) {
            val currentPage = state.pages[listState.firstVisibleItemIndex]
            val isLastPage = listState.firstVisibleItemIndex >= state.pages.size - 1

            // Usamos la URL base del capítulo asumiendo que coincide con la del manga
            val mangaUrlFallback = currentPage.chapter.url.substringBeforeLast("/")

            LibraryManager.saveProgress(
                context = context,
                mangaUrl = mangaUrlFallback,
                chapterUrl = currentPage.chapter.url,
                page = listState.firstVisibleItemIndex,
                isFinished = isLastPage
            )
        }
    }

    // 🔥 3. REANUDAR EN LA PÁGINA CORRECTA (SCROLL AUTOMÁTICO AL ENTRAR)
    LaunchedEffect(state.pages) {
        val chapter = ReaderDataCache.currentChapter
        if (chapter != null && state.pages.isNotEmpty()) {
            val mangaUrlFallback = chapter.url.substringBeforeLast("/")
            val progress = LibraryManager.progressMap.value[mangaUrlFallback]

            if (progress != null && progress.lastChapterUrl == chapter.url && progress.lastPage > 0) {
                // Hacemos scroll directamente a la página donde se quedó
                listState.scrollToItem(progress.lastPage)
            }
        }
    }

    // 4. Limpiamos al salir y manejamos las barras inmersivas
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }

        controller?.let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())

            viewModel.clearReader()
            ReaderDataCache.currentChapter = null
            ReaderDataCache.chapters = emptyList()
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isLoadingNext) {
            viewModel.loadNext()
        }
    }

    val currentVisibleChapterName by remember {
        derivedStateOf {
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
            val firstVisibleIndex = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
            if (state.pages.isNotEmpty() && firstVisibleIndex < state.pages.size) {
                state.pages[firstVisibleIndex]
            } else null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        if (state.isLoadingInitial) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showOverlay = !showOverlay }
            ) {
                itemsIndexed(state.pages, key = { _, page -> page.uniqueId }) { index, readerPage ->

                    if (index > 0 && state.pages[index - 1].chapter.url != readerPage.chapter.url) {
                        ChapterTransitionDivider(
                            prevName = cleanChapterName(state.pages[index - 1].chapter.name),
                            nextName = cleanChapterName(readerPage.chapter.name)
                        )
                    }

                    val imageUrl = readerPage.page.imageUrl ?: readerPage.page.url

                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(false)
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

                if (state.isLoadingNext) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }
            }
        }

        if (currentVisiblePageInfo != null) {
            val pageInfo = currentVisiblePageInfo!!

            val (capActual, capTotal) = remember(pageInfo.chapter.url) {
                val total = ReaderDataCache.chapters.size
                val index = ReaderDataCache.chapters.indexOfFirst { it.url == pageInfo.chapter.url }
                val current = if (index != -1) total - index else 0
                Pair(current, total)
            }

            Text(
                text = "Cap. $capActual/$capTotal  •  Pág. ${pageInfo.displayIndex}/${pageInfo.totalPages}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .background(Color(0xFF121212).copy(alpha = 0.8f), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        AnimatedVisibility(
            visible = showOverlay,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .statusBarsPadding()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    Text(
                        text = cleanChapterName(currentVisibleChapterName),
                        color = Color.White,
                        maxLines = 1,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
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