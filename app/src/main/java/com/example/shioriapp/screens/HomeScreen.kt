package com.example.shioriapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Manga(val title: String, val chapter: String)

val recientes = listOf(
    Manga("Black Clover", "Cap. 350"),
    Manga("Jujutsu Kaisen", "Cap. 220"),
    Manga("Chainsaw Man", "Cap. 130")
)

val biblioteca = listOf(
    Manga("One Piece", "Cap. 1080"),
    Manga("Naruto", "Finalizado"),
    Manga("Bleach", "Finalizado"),
    Manga("Boku no Hero", "Cap. 380"),
    Manga("Tokyo Ghoul", "Finalizado"),
    Manga("Dragon Ball", "Finalizado"),
    Manga("Berserk", "Cap. 371"),
    Manga("Vagabond", "En pausa")
)

@Composable
fun HomeScreen() {
    // Usamos LazyVerticalGrid como el contenedor principal de scroll
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- SECCIÓN: LEÍDO RECIENTEMENTE ---
        // Usamos span para que el título ocupe las 2 columnas
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Leído Recientemente",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // El carrusel horizontal también debe ocupar las 2 columnas para no romperse
        item(span = { GridItemSpan(2) }) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                items(recientes.size) { index ->
                    MangaCard(manga = recientes[index], isRecent = true)
                }
            }
        }

        // --- SECCIÓN: BIBLIOTECA ---
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Tu Biblioteca",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )
        }

        // Aquí no usamos span, por lo que se mostrarán 2 por fila automáticamente
        items(biblioteca) { manga ->
            MangaCard(manga = manga, isRecent = false)
        }
    }
}

@Composable
fun MangaCard(manga: Manga, isRecent: Boolean) {
    // Ajustamos el tamaño para que encajen bien en el diseño de 2 columnas
    val width = if (isRecent) 130.dp else 160.dp
    val height = if (isRecent) 190.dp else 230.dp

    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.size(width, height)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Gray),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                Text(
                    text = manga.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = manga.chapter,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}