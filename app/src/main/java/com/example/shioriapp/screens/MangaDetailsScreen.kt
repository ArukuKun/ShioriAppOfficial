package com.example.shioriapp.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
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
import com.example.shioriapp.viewmodel.MangaDetailViewModel
import com.example.shioriapp.R
import com.example.shioriapp.data.repository.LibraryManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailsScreen(
    mangaUrl: String = "",
    sourceName: String = "",
    mangaTitle: String = "Cargando...",
    onBack: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onChapterClick: (com.example.shioriapp.domain.model.ChapterInfo, List<com.example.shioriapp.domain.model.ChapterInfo>) -> Unit = { _, _ -> },
    viewModel: MangaDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val localContext = LocalContext.current

    LaunchedEffect(mangaUrl) {
        LibraryManager.init(localContext) // Asegurar que el gestor este listo
        viewModel.loadMangaDetails(mangaUrl, sourceName, mangaTitle)
    }

    // 🔥 CORRECCIÓN AQUÍ: Evaluamos contra la variable 'sourceName' de la pantalla, no la del manga de red.
    val library by LibraryManager.library.collectAsState()
    val isFavorite = state.manga?.let { manga ->
        library.any { it.url == manga.url && it.sourceName == sourceName }
    } ?: false

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── FONDO DIFUMINADO ──
        AsyncImage(
            model = ImageRequest.Builder(localContext)
                .data(state.manga?.coverUrl)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.3f).blur(25.dp)
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(8.dp)
                                .background(Color.Black.copy(0.4f), RoundedCornerShape(50))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // ── ENCABEZADO ──
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Portada del Manga
                    AsyncImage(
                        model = ImageRequest.Builder(localContext)
                            .data(state.manga?.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .width(120.dp)
                            .aspectRatio(0.7f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        val displayTitle = state.manga?.title?.takeIf { it.isNotBlank() } ?: mangaTitle

                        Text(
                            text = displayTitle,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val statusInt = state.manga?.status ?: 0
                        val (statusText, statusColor) = when (statusInt) {
                            1 -> "En curso" to Color(0xFF4CAF50)
                            2, 4 -> "Completado" to Color(0xFFF44336)
                            6 -> "Pausado" to Color(0xFFFF9800)
                            else -> "Desconocido" to Color.Gray
                        }

                        Surface(
                            color = statusColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Canvas(modifier = Modifier.size(8.dp)) {
                                    drawCircle(color = statusColor, radius = 3.dp.toPx())
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val autor = state.manga?.author
                        if (!autor.isNullOrBlank() && autor.lowercase() != "desconocido") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = autor,
                                    color = Color.White.copy(0.7f),
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                AsyncImage(
                                    model = ImageRequest.Builder(localContext)
                                        .data(R.drawable.ic_launcher_foreground)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(0.1f)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                // ── BOTONES DE ACCIÓN ──
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 🔥 CORRECCIÓN AQUÍ: Inyectar la extensión antes de guardarlo en disco
                    ActionIcon(
                        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        label = if (isFavorite) "En Biblioteca" else "Añadir",
                        onClick = {
                            state.manga?.let { manga ->
                                val mangaToSave = manga.copy(sourceName = sourceName)
                                LibraryManager.toggleManga(localContext, mangaToSave)
                            }
                        }
                    )
                    ActionIcon(Icons.Default.Share, "Compartir")
                    ActionIcon(Icons.Default.Sync, "Migrar")
                    ActionIcon(Icons.Default.FileDownload, "Descargar")
                }

                // ── SINOPSIS ──
                Text(
                    text = "Sinopsis",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Text(
                    text = state.manga?.description ?: "No hay descripción disponible.",
                    color = Color.White.copy(0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = TextAlign.Justify
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── CATEGORÍAS ──
                val tags = state.manga?.genres?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                if (tags.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tags) { tag ->
                            Surface(
                                color = Color.White.copy(0.1f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.clickable { onCategoryClick(tag) }
                            ) {
                                Text(
                                    text = tag,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.White.copy(0.1f))

                // ── LISTA DE CAPÍTULOS ──
                Text(
                    text = "Capítulos (${state.chapters.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(16.dp)
                )

                if (state.isLoading && state.chapters.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                state.chapters.forEach { chapter ->
                    ChapterItem(
                        chapter = chapter,
                        onClick = { onChapterClick(chapter, state.chapters) }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ChapterItem(
    chapter: com.example.shioriapp.domain.model.ChapterInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapter.name,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Subido: ${chapter.dateUpload}",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
        Icon(
            imageVector = Icons.Default.FileDownload,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}