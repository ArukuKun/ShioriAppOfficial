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
import java.io.File

@Composable
fun HomeScreen(
    onMangaClick: (MangaInfo) -> Unit,
    onResumeClick: (MangaInfo) -> Unit = {}
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        LibraryManager.init(context)
    }

    val progressMap by LibraryManager.progressMap.collectAsState()
    val biblioteca by LibraryManager.library.collectAsState()

    val continueReadingList = remember(progressMap) {
        val tempList = mutableListOf<Pair<MangaInfo, Long>>()

        progressMap.forEach { (url, progress) ->
            if (progress.lastChapterUrl.isNotBlank()) {
                val hash = url.hashCode()
                val mangaFile = File(context.cacheDir, "${hash}_manga.json")

                if (mangaFile.exists()) {
                    try {
                        val mObj = org.json.JSONObject(mangaFile.readText())
                        val cachedSource = mObj.optString("sourceName")
                        if (cachedSource.isBlank() || cachedSource == "null") {
                            mangaFile.delete()
                            val capsFile = File(context.cacheDir, "${hash}_caps.json")
                            if (capsFile.exists()) capsFile.delete()
                            throw Exception("Caché corrupto detectado y eliminado")
                        }

                        val manga = MangaInfo(
                            title = mObj.optString("title"),
                            url = url,
                            coverUrl = mObj.optString("coverUrl"),
                            description = mObj.optString("description"),
                            author = mObj.optString("author"),
                            status = mObj.optInt("status"),
                            genres = mObj.optString("genres"),
                            sourceName = cachedSource
                        )

                        tempList.add(Pair(manga, progress.lastReadTime))

                    } catch (e: Exception) {
                        // Caché corrupto, se ignora
                    }
                }
            }
        }
        tempList.sortedByDescending { it.second }.map { it.first }
    }

    // 🔥 GRID ADAPTATIVO QUE SE AJUSTA AL TAMAÑO DE PANTALLA
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 104.dp,  // 🔥 AUMENTADO para evitar colisión con la TopAppBar y la barra de estado
            bottom = 120.dp // 🔥 AUMENTADO para evitar que la pastilla tape contenido
        ),
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
            item(span = { GridItemSpan(maxLineSpan) }) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.Gray.copy(alpha = 0.2f)
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Tu Biblioteca",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (biblioteca.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Tu biblioteca está vacía. Ve a explorar o usa el buscador para añadir mangas.",
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 32.dp)
                )
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
            AsyncImage(
                model = manga.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.9f)), startY = 150f))
            )
            Text(
                manga.title,
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                maxLines = 2
            )
        }
    }
}