package com.example.shioriapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.example.shioriapp.data.repository.LibraryManager
import com.example.shioriapp.navigation.MigrationCache


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MigrateScreen(
    onBack: () -> Unit,
    onNavigateToSearch: (String) -> Unit // Recibe la acción para ir a buscar
) {
    val libraryMangas by LibraryManager.library.collectAsState(initial = emptyList())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Migración Global", fontWeight = FontWeight.Bold) },
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

            Card(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Selecciona un manga de tu biblioteca para migrarlo a otra extensión. El progreso se intentará traspasar.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (libraryMangas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tu biblioteca está vacía.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(libraryMangas, key = { it.url }) { manga ->
                        ListItem(
                            leadingContent = {
                                // 🔥 FOTO DEL ANIME/MANGA ACTIVADA
                                // Nota: Si tu propiedad en MangaInfo se llama diferente a coverUrl, reemplázala aquí
                                AsyncImage(
                                    model = manga.coverUrl,
                                    contentDescription = "Portada de ${manga.title}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp, 64.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            },
                            headlineContent = {
                                Text(manga.title, fontWeight = FontWeight.Bold, maxLines = 1)
                            },
                            supportingContent = {
                                Text(manga.sourceName, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            },
                            trailingContent = {
                                Button(
                                    onClick = {
                                        // 🔥 EMPEZAMOS CON EL BOTÓN: Clavamos el ancla en el caché y mandamos a buscar
                                        MigrationCache.oldManga = manga
                                        onNavigateToSearch(manga.title)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Migrar", fontSize = 12.sp)
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }
}