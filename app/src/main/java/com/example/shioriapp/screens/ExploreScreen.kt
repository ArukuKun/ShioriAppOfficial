package com.example.shioriapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable // 🔥 Importante para guardar estado entre navegaciones
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.shioriapp.viewmodel.ExploreViewModel
import kotlin.random.Random // 🔥 Para usar nuestra semilla

@Composable
fun ExploreScreen(
    onMangaClick: (String, String, String) -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    val context = LocalContext.current

    // 🔥 LA SOLUCIÓN: Generamos un número aleatorio (semilla) UNA SOLA VEZ por sesión.
    // rememberSaveable asegura que este número no se borre al entrar y salir de un manga.
    val sessionSeed = rememberSaveable { Random.nextLong() }

    // 🔥 Usamos la misma semilla para mezclar. Así siempre da el mismo resultado
    // mientras la app esté abierta.
    val heroManga = remember(state.allMangas, sessionSeed) {
        if (state.allMangas.isNotEmpty()) {
            state.allMangas.shuffled(Random(sessionSeed)).firstOrNull()
        } else null
    }

    val customImageLoader = remember {
        coil.ImageLoader.Builder(context)
            .okHttpClient {
                eu.kanade.tachiyomi.network.NetworkHelper(context).client
            }
            .build()
    }

    // 🔥 Usamos la misma semilla para la grilla. Si entras a "Acción", se mezcla con la semilla.
    // Si entras a un manga y vuelves, se mezcla con la MISMA semilla, quedando exactamente igual.
    val gridMangas = remember(state.displayMangas, sessionSeed, heroManga) {
        state.displayMangas
            .filter { it.url != heroManga?.url }
            .shuffled(Random(sessionSeed))
    }

    val backgroundColor = MaterialTheme.colorScheme.background

    if (state.isLoading && state.allMangas.isEmpty()) {
        Box(Modifier.fillMaxSize().background(backgroundColor), Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().background(backgroundColor),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (heroManga != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onMangaClick(heroManga.url, heroManga.sourceName, heroManga.title) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(heroManga.coverUrl ?: "")
                                .crossfade(true)
                                .build(),
                            imageLoader = customImageLoader,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.3f),
                                            backgroundColor.copy(alpha = 0.9f),
                                            backgroundColor
                                        ),
                                        startY = 400f
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = heroManga.title,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Button(
                                onClick = { onMangaClick(heroManga.url, heroManga.sourceName, heroManga.title) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Text("Leer ahora", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    items(state.categories) { category ->
                        val isSelected = state.selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setCategory(category) },
                            label = {
                                Text(
                                    text = category,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = if (state.selectedCategory == "Todo") "Tendencias para ti" else "Explorar ${state.selectedCategory}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            if (state.isLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (gridMangas.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No hay resultados en esta categoría.", color = Color.Gray)
                    }
                }
            } else {
                items(gridMangas) { manga ->
                    MangaGridItem(
                        title = manga.title,
                        coverUrl = manga.coverUrl ?: "",
                        imageLoader = customImageLoader,
                        onClick = { onMangaClick(manga.url, manga.sourceName, manga.title) }
                    )
                }
            }
        }
    }
}

@Composable
fun MangaGridItem(
    title: String,
    coverUrl: String,
    imageLoader: coil.ImageLoader,
    onClick: () -> Unit
) {
    LaunchedEffect(coverUrl) {
        android.util.Log.e("SHIORI_IMAGE_DEBUG", "Intentando cargar: '$coverUrl' para el manga: $title")
    }

    Column(modifier = Modifier.clickable { onClick() }.fillMaxWidth()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(coverUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                .addHeader("Referer", coverUrl)
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onSuccess = {
                android.util.Log.d("SHIORI_IMAGE_DEBUG", "✅ ÉXITO cargando: $title")
            },
            onError = { error ->
                android.util.Log.e("SHIORI_IMAGE_DEBUG", "❌ FALLÓ: $title | Razón: ${error.result.throwable.message}")
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}