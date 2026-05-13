package com.example.shioriapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onMangaClick: (MangaInfo) -> Unit,
    modifier: Modifier = Modifier, // 🔥 SOLUCIÓN AL ERROR: Añadimos el parámetro modifier
    viewModel: SearchViewModel = viewModel()
) {
    val query by viewModel.searchQuery.collectAsState()
    val globalResults by viewModel.globalSearchResults.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.initAllSources(context)
    }

    Scaffold(
        modifier = modifier, // 🔥 Aplicamos el modifier al Scaffold
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        placeholder = { Text("Buscar en todas las fuentes...") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            if (error != null) {
                Text(text = error!!, color = Color.Red, modifier = Modifier.align(Alignment.Center))
            } else if (globalResults.isEmpty() && query.isNotEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(globalResults) { result ->
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                            Text(
                                text = result.sourceName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                            )

                            if (result.mangas == null) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (result.error != null) {
                                Text(
                                    text = result.error,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            } else if (result.mangas!!.isEmpty()) {
                                Text(
                                    text = "No se encontraron resultados.",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(result.mangas!!) { manga ->
                                        // 🔥 Usamos la Card optimizada para búsqueda
                                        SearchMangaCard(
                                            manga = manga,
                                            onClick = {
                                                val mangaConSource = if (manga.sourceName.isBlank())
                                                    manga.copy(sourceName = result.sourceName)
                                                else manga
                                                onMangaClick(mangaConSource)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchMangaCard(manga: MangaInfo, onClick: () -> Unit) {
    val gradient = remember {
        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.9f)), startY = 300f)
    }

    Card(
        modifier = Modifier
            .width(120.dp)
            .aspectRatio(0.7f)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box {
            AsyncImage(
                model = manga.coverUrl, // 🔥 Corregido a coverUrl para coincidir con tu modelo
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(gradient))
            Text(
                text = manga.title,
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}