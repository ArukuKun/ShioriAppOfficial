package com.example.shioriapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onMangaClick: (MangaInfo) -> Unit,
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
                Text(text = "Cargando...", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(globalResults) { result ->
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                            // Título de la fuente / extensión
                            Text(
                                text = result.sourceName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                            )

                            if (result.mangas == null) {
                                // 🔥 ESTADO 1: CARGANDO LA EXTENSIÓN
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (result.error != null) {
                                // 🔥 ESTADO 2: DIO ERROR ESTA EXTENSIÓN
                                Text(
                                    text = result.error,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            } else if (result.mangas.isEmpty()) {
                                // 🔥 ESTADO 3: SIN RESULTADOS
                                Text(
                                    text = "No se encontraron resultados.",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            } else {
                                // 🔥 ESTADO 4: MANGAS CARGADOS
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Agregamos !! para decirle al compilador que aquí mangas no es nulo
                                    items(result.mangas!!) { manga ->
                                        LibraryMangaCard(
                                            manga = manga,
                                            onClick = {
                                                // Aseguramos inyectar la fuente antes de navegar a detalles
                                                val mangaConSource = if (manga.sourceName.isBlank()) manga.copy(sourceName = result.sourceName) else manga
                                                onMangaClick(mangaConSource)
                                            },
                                            modifier = Modifier.width(110.dp) // Ancho fijo para scroll horizontal
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