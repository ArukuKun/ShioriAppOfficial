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

// Agrupa las extensiones para la pantalla principal sin depender de SourceHolder
data class ExtensionGroup(
    val name: String,
    val pkg: String, // Usamos 'pkg' para ser compatibles con ExploreScreen
    val lang: String,
    val sources: List<Source>
)

class SearchViewModel : ViewModel() {

    // Guardamos directamente los Sources nativos, eliminando el problema de compilación
    private var installedSources: List<Source> = emptyList()

    // Estado con las extensiones ya filtradas y agrupadas
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
            // Usamos loadAllExtensions que sí es detectado por el compilador
            installedSources = ExtensionLoader.loadAllExtensions(context)

            _installedExtensions.value = installedSources.groupBy { it.name }
                .map { (name, sourcesList) ->
                    val first = sourcesList.first()
                    ExtensionGroup(
                        name = name,
                        pkg = "tachiyomi.extension.${name.lowercase().replace(" ", "")}",
                        // Si hay más de 1 fuente con el mismo nombre, es multi-idioma (ALL)
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
                // Buscamos en todas las extensiones instaladas concurrentemente
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
}