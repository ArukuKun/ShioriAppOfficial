package com.example.shioriapp.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.viewmodel.ExploreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onMangaClick: (String, String, String) -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val heroManga = remember(state.allMangas) { state.allMangas.firstOrNull() }

    val customImageLoader = remember {
        coil.ImageLoader.Builder(context)
            .okHttpClient { eu.kanade.tachiyomi.network.NetworkHelper(context).client }
            .build()
    }

    val gridMangas = remember(state.displayMangas, heroManga) {
        state.displayMangas.filter { it.url != heroManga?.url }.distinctBy { it.url }
    }

    val backgroundColor = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        if (state.isLoading && state.allMangas.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyVerticalGrid(
                // 🔥 GRID ADAPTIVO EN LUGAR DE FIXED(3)
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 120.dp, // 🔥 AUMENTADO para evitar que la pastilla tape contenido
                    top = 80.dp // 🔥 REDUCIDO de 100.dp a 80.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // HERO MANGA
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
                                    .data(heroManga.coverUrl ?: "").crossfade(true).build(),
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
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f), backgroundColor.copy(alpha = 0.9f), backgroundColor),
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
                                    text = heroManga.title, color = Color.White, fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Button(
                                    onClick = { onMangaClick(heroManga.url, heroManga.sourceName, heroManga.title) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth(0.6f)
                                ) {
                                    Text("Leer ahora", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }

                // CATEGORÍAS
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        items(state.categories) { category ->
                            val isSelected = state.selectedCategory == category
                            FilterChip(
                                selected = isSelected, onClick = { viewModel.setCategory(category) },
                                label = { Text(category, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = if (state.selectedCategory == "Todo") "Tendencias para ti" else "Explorar ${state.selectedCategory}",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                // GRILLA DE MANGAS GLOBAL
                if (state.isLoading && state.allMangas.isEmpty()) {
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
                    itemsIndexed(items = gridMangas, key = { _, manga -> manga.url }) { index, manga ->
                        MangaGridItem(
                            title = manga.title, coverUrl = manga.coverUrl ?: "",
                            imageLoader = customImageLoader, onClick = { onMangaClick(manga.url, manga.sourceName, manga.title) }
                        )
                        if (index >= gridMangas.lastIndex - 6 && !state.isLoadingMore && !state.isLastPage) {
                            LaunchedEffect(gridMangas.size) { viewModel.loadMoreMangas() }
                        }
                    }
                }

                if (state.isLoadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }

        // 🔥 POPUP INMERSIVO MODAL
        if (state.isShowingSources) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = {
                    viewModel.toggleSourcesView(false)
                    viewModel.closeExtensionCatalog()
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.background,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                if (state.selectedExtension == null) {
                    Text(
                        text = "Tus Extensiones",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
                    )
                    SourcesList(
                        sources = state.availableSources,
                        onSourceClick = { sourceName -> viewModel.openExtensionCatalog(sourceName) }
                    )
                } else {
                    ExtensionCatalogView(
                        sourceName = state.selectedExtension!!,
                        mangas = state.extensionMangas,
                        isLoading = state.isExtensionLoading,
                        isLoadingMore = state.isExtensionLoadingMore,
                        selectedTab = state.extensionTab,
                        onTabSelected = { viewModel.setExtensionTab(it) },
                        onBack = { viewModel.closeExtensionCatalog() },
                        onMangaClick = { manga ->
                            onMangaClick(manga.url, manga.sourceName, manga.title)
                        },
                        imageLoader = customImageLoader,
                        onLoadMore = { viewModel.loadMoreExtensionMangas() }
                    )
                }
            }
        }
    }
}

@Composable
fun ExtensionCatalogView(
    sourceName: String,
    mangas: List<MangaInfo>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onBack: () -> Unit,
    onMangaClick: (MangaInfo) -> Unit,
    imageLoader: coil.ImageLoader,
    onLoadMore: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val sourceIcon = remember(sourceName) { getExtensionIcon(packageManager, sourceName) }

    val distinctMangas = remember(mangas) { mangas.distinctBy { it.url } }

    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }

            if (sourceIcon != null) {
                Image(
                    bitmap = sourceIcon.toBitmap().asImageBitmap(), contentDescription = null,
                    modifier = Modifier.size(28.dp).clip(CircleShape).padding(end = 6.dp)
                )
            }

            Text(
                text = sourceName, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = { }) { Icon(Icons.Default.FilterList, contentDescription = "Filtros") }
            IconButton(onClick = { }) { Icon(Icons.Default.Search, contentDescription = "Buscar") }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            val tabs = listOf("Populares", "Recientes", "Todo")
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index, onClick = { onTabSelected(index) },
                    text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                )
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else if (distinctMangas.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No se encontraron resultados.", color = Color.Gray) }
        } else {
            // 🔥 GRID ADAPTIVO
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(items = distinctMangas, key = { _, manga -> manga.url }) { index, manga ->
                    MangaGridItem(
                        title = manga.title, coverUrl = manga.coverUrl ?: "",
                        imageLoader = imageLoader, onClick = { onMangaClick(manga) }
                    )

                    if (index >= distinctMangas.lastIndex - 6 && !isLoadingMore) {
                        LaunchedEffect(distinctMangas.size) { onLoadMore() }
                    }
                }

                if (isLoadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourcesList(sources: List<String>, onSourceClick: (String) -> Unit) {
    val context = LocalContext.current
    val packageManager = context.packageManager

    if (sources.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("No tienes extensiones instaladas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sources) { sourceName ->
                val sourceIcon = remember(sourceName) { getExtensionIcon(packageManager, sourceName) }

                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSourceClick(sourceName) },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (sourceIcon != null) {
                                Image(
                                    bitmap = sourceIcon.toBitmap().asImageBitmap(), contentDescription = null,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                )
                            } else {
                                Icon(Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sourceName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Extensión instalada", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun getExtensionIcon(packageManager: PackageManager, sourceName: String): android.graphics.drawable.Drawable? {
    return try {
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val extensionPackage = packages.firstOrNull { appInfo ->
            val pkgName = appInfo.packageName
            (pkgName.contains("eu.kanade.tachiyomi.extension") ||
                    pkgName.contains("eu.kanade.tachiyomi.animeextension") ||
                    pkgName.contains("aniyomi") ||
                    pkgName.contains("keiyoushin.extension")) &&
                    packageManager.getApplicationLabel(appInfo).toString().contains(sourceName, ignoreCase = true)
        }
        extensionPackage?.loadIcon(packageManager)
    } catch (e: Exception) { null }
}

@Composable
fun MangaGridItem(title: String, coverUrl: String, imageLoader: coil.ImageLoader, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }.fillMaxWidth()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(coverUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                .addHeader("Referer", coverUrl).crossfade(true).build(),
            imageLoader = imageLoader, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.7f).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}