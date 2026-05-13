package com.example.shioriapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.viewmodel.ExploreViewModel

@Composable
fun ExploreScreen(
    onMangaClick: (String, String, String) -> Unit,
    viewModel: ExploreViewModel = viewModel()
){
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        Spacer(modifier = Modifier.height(16.dp))

        // ── CATEGORÍAS ──
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // 🔥 OPTIMIZACIÓN 1: Llave única para las categorías
            items(
                items = state.categories,
                key = { category -> category }
            ) { category ->
                val isSelected = state.selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setCategory(category) },
                    label = { Text(category) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── GRILLA ──
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else if (state.displayMangas.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No hay resultados.", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                // 🔥 Aseguramos que ocupe el resto del espacio disponible correctamente
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                // 🔥 OPTIMIZACIÓN 2: Llave única usando la URL del manga
                items(
                    items = state.displayMangas,
                    key = { manga -> manga.url }
                ) { manga ->
                    MangaGridItem(
                        manga = manga, // Pasamos el objeto completo
                        onClick = {
                            // Lógica de navegación restaurada
                            onMangaClick(manga.url, manga.sourceName, manga.title)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MangaGridItem(manga: MangaInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = manga.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .background(Color.DarkGray)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = manga.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            color = Color.White,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}