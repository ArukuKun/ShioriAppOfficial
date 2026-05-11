package com.example.shioriapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shioriapp.core.util.ExtensionLoader
import com.example.shioriapp.domain.model.ChapterInfo
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.domain.source.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ExtensionGroup(
    val name: String,
    val pkg: String,
    val lang: String,
    val sources: List<Source>
)

data class SourceSearchResult(
    val sourceName: String,
    val mangas: List<MangaInfo>? = null, // Si es null, mostrará el spinner de cargando
    val error: String? = null
)

class SearchViewModel : ViewModel() {

    private var installedSources: List<Source> = emptyList()

    private val _installedExtensions = MutableStateFlow<List<ExtensionGroup>>(emptyList())
    val installedExtensions: StateFlow<List<ExtensionGroup>> = _installedExtensions.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _globalSearchResults = MutableStateFlow<List<SourceSearchResult>>(emptyList())
    val globalSearchResults: StateFlow<List<SourceSearchResult>> = _globalSearchResults.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val searchMutex = Mutex()

    // 🔥 Variables para controlar la búsqueda automática mientras escribes
    private var typingJob: Job? = null
    private var searchJob: Job? = null

    fun initAllSources(context: Context) {
        if (installedSources.isEmpty()) {
            installedSources = ExtensionLoader.loadAllExtensions(context)

            val installedPkgs = context.packageManager
                .getInstalledPackages(0)
                .map { it.packageName }
                .filter { it.startsWith("eu.kanade.tachiyomi.extension") }

            _installedExtensions.value = installedSources.groupBy { it.name }
                .map { (name, sourcesList) ->
                    val first = sourcesList.first()
                    val realPkg = installedPkgs.firstOrNull { pkg ->
                        pkg.contains(name.lowercase().replace(" ", ""), ignoreCase = true)
                    } ?: "eu.kanade.tachiyomi.extension.${first.lang}.${name.lowercase().replace(" ", "")}"

                    ExtensionGroup(
                        name = name,
                        pkg = realPkg,
                        lang = if (sourcesList.size > 1) "ALL" else first.lang.uppercase(),
                        sources = sourcesList
                    )
                }
        }
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query

        // 🔥 Cancelamos el temporizador anterior y la búsqueda en curso cada vez que tocas una tecla
        typingJob?.cancel()
        searchJob?.cancel()

        // Si borraste todo el texto, limpiamos la pantalla de inmediato
        if (query.isBlank()) {
            _globalSearchResults.value = emptyList()
            return
        }

        // 🔥 Iniciamos un nuevo temporizador. Si pasas 700ms sin teclear, se dispara la búsqueda.
        typingJob = viewModelScope.launch {
            delay(700)
            search()
        }
    }

    fun search() {
        val query = _searchQuery.value
        if (installedSources.isEmpty()) {
            _error.value = "No hay ninguna extensión instalada."
            return
        }
        if (query.isBlank()) return

        _error.value = null

        // Por seguridad, si el usuario apretó el botón de buscar manual, cancelamos la auto-búsqueda
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            // 1. Cargamos todas las fuentes disponibles en la UI con estado "Cargando"
            val initialResults = installedSources.map { source ->
                SourceSearchResult(sourceName = source.name, mangas = null, error = null)
            }
            _globalSearchResults.value = initialResults

            // 2. Disparamos la búsqueda. Al estar dentro de este launch, si el usuario
            // vuelve a teclear, todas estas tareas de red se cancelan automáticamente ahorrando internet.
            installedSources.forEach { source ->
                launch(Dispatchers.IO) {
                    try {
                        val results = source.fetchSearchManga(query, 1)
                        updateSourceResult(source.name, results, null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        updateSourceResult(source.name, emptyList(), "Error al buscar: ${e.message}")
                    }
                }
            }
        }
    }

    private suspend fun updateSourceResult(sourceName: String, mangas: List<MangaInfo>, error: String?) {
        searchMutex.withLock {
            val currentList = _globalSearchResults.value.toMutableList()
            val index = currentList.indexOfFirst { it.sourceName == sourceName }
            if (index != -1) {
                currentList[index] = currentList[index].copy(
                    mangas = mangas,
                    error = error
                )
                _globalSearchResults.value = currentList
            }
        }
    }

    // --- MANEJO DE DETALLES Y CAPÍTULOS INTACTOS ---
    private val _detailManga = MutableStateFlow<MangaInfo?>(null)
    val detailManga: StateFlow<MangaInfo?> = _detailManga.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterInfo>>(emptyList())
    val chapters: StateFlow<List<ChapterInfo>> = _chapters

    private val _isLoadingChapters = MutableStateFlow(false)
    val isLoadingChapters: StateFlow<Boolean> = _isLoadingChapters

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    fun fetchDetails(context: Context, manga: MangaInfo) {
        _detailManga.value = manga
        _isLoadingDetail.value = true
        _isLoadingChapters.value = true
        _chapters.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val realSource = ExtensionLoader.getSource(manga.sourceName)
                if (realSource != null) {
                    kotlinx.coroutines.coroutineScope {
                        val detailsDeferred = async { realSource.fetchMangaDetails(manga) }
                        val chaptersDeferred = async { realSource.fetchChapterList(manga) }

                        val fullDetails = detailsDeferred.await()
                        val chapterList = chaptersDeferred.await()

                        _detailManga.value = fullDetails
                        _chapters.value = chapterList
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingDetail.value = false
                _isLoadingChapters.value = false
            }
        }
    }

    fun clearDetail() { _detailManga.value = null }

    private val _readingChapter = MutableStateFlow<ChapterInfo?>(null)
    val readingChapter: StateFlow<ChapterInfo?> = _readingChapter

    fun openReader(chapter: ChapterInfo) { _readingChapter.value = chapter }
    fun closeReader() { _readingChapter.value = null }
}