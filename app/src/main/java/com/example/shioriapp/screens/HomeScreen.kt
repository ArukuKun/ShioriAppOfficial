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
fun HomeScreen(onMangaClick: (MangaInfo) -> Unit) {
    val context = LocalContext.current
    // Recolectamos el estado de la biblioteca
    val biblioteca by LibraryManager.library.collectAsState()

    LaunchedEffect(Unit) {
        LibraryManager.init(context)
    }

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
                Text("Tu biblioteca está vacía.", color = Color.Gray, modifier = Modifier.padding(top = 32.dp))
            }
        } else {
            items(
                items = biblioteca,
                key = { it.url } // 🔥 Clave única para evitar recomposiciones
            ) { manga ->
                LibraryMangaCard(
                    manga = manga,
                    onClick = { onMangaClick(manga) }
                )
            }
        }
    }
}

@Composable
fun LibraryMangaCard(manga: MangaInfo, onClick: () -> Unit) {
    val gradient = remember { Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.9f)), startY = 300f) }

    Card(
        modifier = Modifier.aspectRatio(0.7f).clickable { onClick() },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box {
            AsyncImage(
                model = manga.coverUrl, // 🔥 Nombre corregido
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(gradient))
            Text(manga.title, color = Color.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp), maxLines = 2)
        }
    }
}