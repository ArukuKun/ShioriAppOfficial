package com.example.shioriapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shioriapp.core.util.ExtensionLoader
import com.example.shioriapp.core.util.SourceHolder
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.domain.source.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// NUEVO: Agrupa las extensiones para la pantalla principal
data class ExtensionGroup(
    val name: String,
    val pkgName: String,
    val lang: String,
    val sources: List<Source>
)

class SearchViewModel : ViewModel() {
    private var sourceHolders: List<SourceHolder> = emptyList()

    // Estado con las extensiones ya filtradas y agrupadas
    private val _installedExtensions = MutableStateFlow<List<ExtensionGroup>>(emptyList())
    val installedExtensions = _installedExtensions.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _mangas = MutableStateFlow<List<MangaInfo>>(emptyList())
    val mangas = _mangas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun initAllSources(context: Context) {
        if (sourceHolders.isEmpty()) {
            sourceHolders = ExtensionLoader.loadAllExtensionHolders(context)

            _installedExtensions.value = sourceHolders.groupBy { it.pkgName }
                .map { (pkg, holders) ->
                    val first = holders.first().source
                    ExtensionGroup(
                        name = first.name,
                        pkgName = pkg,
                        // Si el paquete tiene más de 1 fuente, es multi-idioma (ALL)
                        lang = if (holders.size > 1) "ALL" else first.lang.uppercase(),
                        sources = holders.map { it.source }
                    )
                }
        }
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun search() {
        val query = _searchQuery.value
        if (sourceHolders.isEmpty()) {
            _error.value = "No hay ninguna extensión instalada."
            return
        }
        if (query.isBlank()) return

        _isLoading.value = true
        _error.value = null
        _mangas.value = emptyList()

        viewModelScope.launch {
            try {
                // Buscamos en todas las extensiones instaladas
                val deferredResults = sourceHolders.map { holder ->
                    async(Dispatchers.IO) {
                        try {
                            holder.source.fetchSearchManga(query, 1)
                        } catch (e: Exception) {
                            emptyList()
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