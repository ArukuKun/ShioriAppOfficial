package com.example.shioriapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
    onMangaClick: (MangaInfo) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        LibraryManager.init(context)
    }

    val biblioteca by LibraryManager.library.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Tu Biblioteca", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
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
                        // 🔥 SISTEMA DE AUTOCURACIÓN: Elimina mangas de pruebas anteriores que se guardaron mal
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