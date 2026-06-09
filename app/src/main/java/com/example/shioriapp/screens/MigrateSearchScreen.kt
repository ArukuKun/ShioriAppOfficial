package com.example.shioriapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.shioriapp.core.util.ExtensionLoader
import com.example.shioriapp.data.repository.LibraryManager
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.navigation.MigrationCache
import com.example.shioriapp.viewmodel.MigrateSearchState
import com.example.shioriapp.viewmodel.MigrateSearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MigrateSearchScreen(
    query: String,
    onBack: () -> Unit,
    onMigrationComplete: (MangaInfo) -> Unit,
    viewModel: MigrateSearchViewModel = viewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val oldManga = MigrationCache.oldManga
    var showDialog by remember { mutableStateOf(false) }
    var selectedTargetManga by remember { mutableStateOf<MangaInfo?>(null) }

    // 🔥 Nuevo estado para que puedas editar el nombre del manga si las fuentes lo escriben distinto
    var searchQuery by remember { mutableStateOf(query) }

    val searchState by viewModel.state.collectAsState()

    // Búsqueda inicial automática
    LaunchedEffect(Unit) {
        if (oldManga != null) {
            viewModel.searchGlobal(query = searchQuery, originalSourceName = oldManga.sourceName)
        }
    }

    // Función para disparar la búsqueda manual
    val performSearch = {
        if (oldManga != null && searchQuery.isNotBlank()) {
            keyboardController?.hide()
            viewModel.searchGlobal(query = searchQuery, originalSourceName = oldManga.sourceName)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Buscando alternativas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Origen: ${oldManga?.sourceName ?: ""}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            // 🔥 LA BARRA SALVAVIDAS: Permite acortar el título a "Alya" si "Roshidere" no funciona
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Editar título para buscar") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                trailingIcon = {
                    IconButton(onClick = { performSearch() }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar de nuevo")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (searchState) {
                is MigrateSearchState.Idle, is MigrateSearchState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Analizando repositorios...", color = Color.Gray)
                        }
                    }
                }
                is MigrateSearchState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val msg = (searchState as MigrateSearchState.Error).message
                        Text(msg, color = MaterialTheme.colorScheme.error)
                    }
                }
                is MigrateSearchState.Success -> {
                    val results = (searchState as MigrateSearchState.Success).results

                    if (results.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontraron coincidencias.\nPrueba acortando el título arriba.", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(results, key = { "${it.sourceName}_${it.url}" }) { targetManga ->
                                MigrationResultCard(
                                    manga = targetManga,
                                    onSelect = {
                                        selectedTargetManga = targetManga
                                        showDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog && oldManga != null && selectedTargetManga != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Confirmar Migración?") },
            text = {
                Text("Vas a migrar tu biblioteca y progreso de:\n" +
                        "👉 ${oldManga.sourceName}\n\nHacia la nueva extensión:\n" +
                        "🎯 ${selectedTargetManga!!.sourceName}\n\nEsta acción modificará tu base de datos local reemplazando el origen.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showDialog = false
                        val target = selectedTargetManga!!
                        LibraryManager.migrateManga(context, oldManga, target)
                        onMigrationComplete(target)
                    }
                ) {
                    Text("Migrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ── COMPONENTE DE TARJETA (El mismo de antes) ──
@Composable
fun MigrationResultCard(
    manga: MangaInfo,
    onSelect: () -> Unit
) {
    var chapterCount by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(manga.url) {
        withContext(Dispatchers.IO) {
            try {
                val source = ExtensionLoader.getSource(manga.sourceName)
                val chapters = source?.fetchChapterList(manga)
                chapterCount = chapters?.size ?: 0
            } catch (e: Exception) {
                chapterCount = -1
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(80.dp, 115.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (manga.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = manga.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Book, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = manga.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(text = manga.sourceName, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Badge(text = getFormatFromGenres(manga.genres), color = MaterialTheme.colorScheme.secondary)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Estado: ${getStatusText(manga.status)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Capítulos: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (chapterCount == null) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    } else if (chapterCount == -1) {
                        Text("Error", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    } else {
                        Text("$chapterCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onSelect,
                    modifier = Modifier.align(Alignment.End).height(34.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text("Elegir", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

fun getStatusText(status: Int): String {
    return when (status) {
        1 -> "En emisión"
        2 -> "Completado"
        3 -> "Licenciado"
        4 -> "Cancelado"
        5 -> "Pausado"
        else -> "Desconocido"
    }
}

fun getFormatFromGenres(genres: String): String {
    val lower = genres.lowercase()
    return when {
        lower.contains("novel") || lower.contains("novela") -> "Novela"
        lower.contains("manhwa") -> "Manhwa"
        lower.contains("manhua") -> "Manhua"
        lower.contains("comic") -> "Cómic"
        lower.contains("manga") -> "Manga"
        else -> "Manga"
    }
}