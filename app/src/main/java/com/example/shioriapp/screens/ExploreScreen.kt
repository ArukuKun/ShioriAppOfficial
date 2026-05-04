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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: SearchViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.mangas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val installedExtensions by viewModel.installedExtensions.collectAsState()

    // ── Detalle desde ViewModel ─────────────────────────────────────────
    val detailManga by viewModel.detailManga.collectAsState()
    val isLoadingDetail by viewModel.isLoadingDetail.collectAsState()

    var selectedCategory by remember { mutableStateOf("Todo") }
    val categorias = listOf("Todo", "Acción", "Aventura", "Comedia", "Drama", "Fantasía", "Terror", "Romance")

    LaunchedEffect(Unit) {
        viewModel.initAllSources(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Buscar en extensiones instaladas...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    viewModel.search()
                }
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(categorias) { categoria ->
                        FilterChip(
                            selected = selectedCategory == categoria,
                            onClick = { selectedCategory = categoria },
                            label = { Text(categoria) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            when {
                isLoading -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                error != null -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = error ?: "Error",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                searchResults.isNotEmpty() -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Resultados",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(searchResults) { manga ->
                        MangaCoverCard(
                            manga = manga,
                            onClick = { viewModel.fetchDetails(manga) }
                        )
                    }
                }
                else -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Tus Extensiones",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(items = installedExtensions, span = { GridItemSpan(maxLineSpan) }) { extension ->
                        ExtensionCard(
                            nombre = extension.name,
                            idioma = extension.lang,
                            pkgName = extension.pkg,
                            onClick = {}
                        )
                    }
                }
            }
        }
    }

    // ── Bottom Sheet a pantalla completa ────────────────────────────────
    if (detailManga != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearDetail() },
            containerColor = MaterialTheme.colorScheme.surface,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = null,
            modifier = Modifier.fillMaxHeight()
        ) {
            MangaDetailContent(
                manga = detailManga!!,
                isLoading = isLoadingDetail,
                onReadClick = {
                    viewModel.clearDetail()
                    /* TODO: Navegar al lector */
                },
                onDismiss = { viewModel.clearDetail() }
            )
        }
    }
}

// ── Detalle estilo Mihon ─────────────────────────────────────────────────
@Composable
fun MangaDetailContent(
    manga: MangaInfo,
    isLoading: Boolean,
    onReadClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var descriptionExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── HEADER: cover difuminado de fondo ────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            // Fondo difuminado
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(manga.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(24.dp)
            )
            // Overlay oscuro
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )
            // Botón cerrar arriba a la derecha
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text("✕", color = Color.White, fontSize = 18.sp)
            }
            // Contenido del header: portada + info
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Portada nítida
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(manga.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = manga.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(110.dp)
                        .height(160.dp)
                        .shadow(8.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                )
                // Textos de info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = manga.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 22.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Autor (shimmer si está cargando)
                    if (isLoading && manga.author.isEmpty()) {
                        ShimmerBox(width = 0.6f, height = 12.dp)
                    } else if (manga.author.isNotEmpty()) {
                        Text(
                            text = manga.author,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Estado
                    val (statusText, statusColor) = when (manga.status) {
                        1 -> "En emisión" to Color(0xFF4CAF50)
                        2 -> "Finalizado" to Color(0xFF2196F3)
                        else -> "Desconocido" to Color.White.copy(alpha = 0.45f)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusColor)
                        )
                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                    // Chip de fuente
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = manga.sourceName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // ── BOTÓN LEER ───────────────────────────────────────────────────
        Button(
            onClick = onReadClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Empezar a leer", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        // ── SINOPSIS ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Sinopsis",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                // Shimmer de 3 líneas mientras carga
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerBox(width = 1f, height = 13.dp)
                    ShimmerBox(width = 1f, height = 13.dp)
                    ShimmerBox(width = 0.65f, height = 13.dp)
                }
            } else {
                val description = manga.description
                    .ifEmpty { "No hay descripción disponible para este manga." }

                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    lineHeight = 20.sp,
                    maxLines = if (descriptionExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
                if (description.length > 200) {
                    Text(
                        text = if (descriptionExpanded) "Ver menos" else "Ver más",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clickable { descriptionExpanded = !descriptionExpanded }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ── Shimmer placeholder ──────────────────────────────────────────────────
@Composable
private fun ShimmerBox(width: Float, height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(width)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
    )
}

// ── Cover card de resultados ─────────────────────────────────────────────
@Composable
fun MangaCoverCard(manga: MangaInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = manga.coverUrl,
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                            startY = 150f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = manga.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Card de extensión instalada ──────────────────────────────────────────
@Composable
fun ExtensionCard(nombre: String, idioma: String, pkgName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("https://raw.githubusercontent.com/keiyoushi/extensions/repo/icon/${pkgName}.png")
                    .crossfade(true)
                    .build(),
                contentDescription = "Icono de $nombre",
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(Icons.Default.Extension),
                placeholder = rememberVectorPainter(Icons.Default.Extension),
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "Idioma: $idioma",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}