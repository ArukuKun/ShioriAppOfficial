package com.example.shioriapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.data.repository.LibraryManager

@Composable
fun HomeScreen(
    onMangaClick: (MangaInfo) -> Unit,
    onResumeClick: (MangaInfo) -> Unit = {}
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        LibraryManager.init(context)
    }

    val biblioteca by LibraryManager.library.collectAsState()
    val progressMap by LibraryManager.progressMap.collectAsState()

    // 🔥 ALGORITMO: Filtra y ordena los mangas en tiempo real
    val continueReadingList = remember(biblioteca, progressMap) {
        biblioteca.filter { manga ->
            val progress = progressMap[manga.url] ?: return@filter false

            // ¿Se terminó de leer el último capítulo guardado?
            val isLastChapterFinished = progress.lastChapterUrl in progress.readChapters
            // ¿Hay capítulos nuevos o sin leer disponibles?
            val hasUnreadChapters = progress.totalChapters > progress.readChapters.size

            // Aparecerá si dejaste un capítulo a medias O si hay capítulos sin leer
            !isLastChapterFinished || hasUnreadChapters
        }.sortedByDescending { manga ->
            progressMap[manga.url]?.lastReadTime ?: 0L
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (continueReadingList.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Seguir Leyendo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(continueReadingList) { manga ->
                        LibraryMangaCard(
                            manga = manga,
                            onClick = { onResumeClick(manga) },
                            modifier = Modifier.width(110.dp)
                        )
                    }
                }
            }
            // Divisor estético
            item(span = { GridItemSpan(maxLineSpan) }) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))
            }
        }

        // SECCIÓN: Tu Biblioteca General
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Tu Biblioteca", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        }

        if (biblioteca.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Tu biblioteca está vacía. Ve a explorar o usa el buscador para añadir mangas.", color = Color.Gray, modifier = Modifier.padding(top = 32.dp))
            }
        } else {
            items(biblioteca) { manga ->
                LibraryMangaCard(
                    manga = manga,
                    onClick = {
                        if (manga.sourceName.isBlank()) {
                            LibraryManager.toggleManga(context, manga)
                            Toast.makeText(context, "Manga corrupto eliminado. Búscalo de nuevo para leerlo.", Toast.LENGTH_LONG).show()
                        } else {
                            onMangaClick(manga)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun LibraryMangaCard(manga: MangaInfo, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.aspectRatio(0.7f).clickable { onClick() },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box {
            AsyncImage(model = manga.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.9f)), startY = 150f)))
            Text(manga.title, color = Color.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp), maxLines = 2)
        }
    }
}