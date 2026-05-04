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
                        pkg = realPkg, // ✅ Ahora usa el pkg real
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
        val query = _searchQuery.value
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
                            source.fetchSearchManga(query, 1)
                        } catch (e: Exception) {
                            emptyList() // Si una falla, no crashea las demás
                        }
                    }
                }

                val allResults = deferredResults.awaitAll().flatten()
                    .distinctBy { it.url } // Elimina mangas duplicados con la misma URL

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
                val source = installedSources.firstOrNull { it.name == manga.sourceName }

                // 👇 LOG 1: ¿Encuentra la fuente?
                android.util.Log.d("SHIORI_DETAIL", "Buscando fuente: '${manga.sourceName}'")
                android.util.Log.d("SHIORI_DETAIL", "Fuentes disponibles: ${installedSources.map { it.name }}")

                if (source != null) {
                    val detailed = source.fetchMangaDetails(manga)

                    // 👇 LOG 2: ¿Qué devuelve?
                    android.util.Log.d("SHIORI_DETAIL", "Descripción: '${detailed.description}'")
                    android.util.Log.d("SHIORI_DETAIL", "Autor: '${detailed.author}'")
                    android.util.Log.d("SHIORI_DETAIL", "Status: ${detailed.status}")

                    _detailManga.value = detailed
                } else {
                    android.util.Log.e("SHIORI_DETAIL", "❌ No encontró la fuente '${manga.sourceName}'")
                }
            } catch (e: Exception) {
                android.util.Log.e("SHIORI_DETAIL", "❌ Error: ${e.message}", e)
            } finally {
                _isLoadingDetail.value = false
            }
        }
    }

    fun clearDetail() {
        _detailManga.value = null
    }


}

