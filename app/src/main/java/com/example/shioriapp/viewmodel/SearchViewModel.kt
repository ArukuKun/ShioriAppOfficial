package com.example.shioriapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shioriapp.core.util.ExtensionLoader
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.domain.source.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExtensionGroup(
    val name: String,
    val pkg: String,
    val lang: String,
    val sources: List<Source>
)

class SearchViewModel : ViewModel() {

    private var installedSources: List<Source> = emptyList()

    private val _installedExtensions = MutableStateFlow<List<ExtensionGroup>>(emptyList())
    val installedExtensions: StateFlow<List<ExtensionGroup>> = _installedExtensions.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _mangas = MutableStateFlow<List<MangaInfo>>(emptyList())
    val mangas: StateFlow<List<MangaInfo>> = _mangas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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
    }

    fun search() {
        val query = _searchQuery.value.trim()
        if (installedSources.isEmpty()) {
            _error.value = "No hay ninguna extensión instalada."
            return
        }
        if (query.isBlank()) return

        _isLoading.value = true
        _error.value = null
        _mangas.value = emptyList()

        viewModelScope.launch {
            try {
                val deferredResults = installedSources.map { source ->
                    async(Dispatchers.IO) {
                        try {
                            val results = source.fetchSearchManga(query, 1)
                            // ✅ EL TRUCO: Forzamos a que cada manga sepa exactamente de qué fuente viene
                            results.map { it.copy(sourceName = source.name) }
                        } catch (e: Exception) {
                            emptyList<MangaInfo>()
                        }
                    }
                }

                val allResults = deferredResults.awaitAll().flatten()
                    .distinctBy { it.url }

                _mangas.value = allResults

                if (allResults.isEmpty()) {
                    _error.value = "Ninguna extensión encontró: $query"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Error crítico durante la búsqueda global."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _detailManga = MutableStateFlow<MangaInfo?>(null)
    val detailManga: StateFlow<MangaInfo?> = _detailManga.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    fun fetchDetails(manga: MangaInfo) {
        viewModelScope.launch {
            _isLoadingDetail.value = true
            _detailManga.value = manga

            try {
                val source = installedSources.firstOrNull {
                    it.name.equals(manga.sourceName, ignoreCase = true)
                }

                if (source != null) {
                    val detailed = withContext(Dispatchers.IO) {
                        source.fetchMangaDetails(manga)
                    }

                    // ✅ Filtrado para ignorar cuando las extensiones devuelven la palabra "null"
                    val desc = detailed.description.takeIf { !it.isNullOrBlank() && it.lowercase() != "null" } ?: manga.description
                    val author = detailed.author.takeIf { !it.isNullOrBlank() && it.lowercase() != "null" } ?: manga.author
                    val cover = detailed.coverUrl.takeIf { !it.isNullOrBlank() && it.lowercase() != "null" } ?: manga.coverUrl

                    val updatedManga = manga.copy(
                        description = desc,
                        author = author,
                        status = if (detailed.status != 0) detailed.status else manga.status,
                        coverUrl = cover
                    )

                    _detailManga.value = updatedManga
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingDetail.value = false
            }
        }
    }

    fun clearDetail() {
        _detailManga.value = null
    }
}