package com.example.shioriapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.shioriapp.domain.model.MangaInfo // 🔥 ASEGÚRATE DE QUE ESTE IMPORT ESTÉ BIEN

@Composable
fun HomeScreen(
    onMangaClick: (MangaInfo) -> Unit // 🔥 Pedimos el objeto completo
) {
    val recientes = emptyList<MangaInfo>()
    val biblioteca = emptyList<MangaInfo>()

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

        // Simulación de biblioteca vacía o con datos
        if (biblioteca.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Tu biblioteca está vacía.", color = Color.Gray, modifier = Modifier.padding(top = 32.dp))
            }
        } else {
            items(biblioteca) { manga ->
                LibraryMangaCard(manga = manga, onClick = { onMangaClick(manga) })
            }
        }
    }
}

@Composable
fun LibraryMangaCard(manga: MangaInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.7f).clickable { onClick() },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box {
            AsyncImage(model = manga.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.9f)), startY = 150f)))
            Text(manga.title, color = Color.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp), maxLines = 2)
        }
    }
}