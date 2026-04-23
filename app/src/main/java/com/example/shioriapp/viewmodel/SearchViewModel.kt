package com.example.shioriapp.viewmodels

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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private var sources: List<Source> = emptyList()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _mangas = MutableStateFlow<List<MangaInfo>>(emptyList())
    val mangas = _mangas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // Carga TODAS las extensiones
    fun initAllSources(context: Context) {
        if (sources.isEmpty()) {
            sources = ExtensionLoader.loadAllExtensions(context)
        }
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun search() {
        val query = _searchQuery.value

        if (sources.isEmpty()) {
            _error.value = "No hay ninguna extensión instalada."
            return
        }
        if (query.isBlank()) return

        _isLoading.value = true
        _error.value = null
        _mangas.value = emptyList()

        viewModelScope.launch {
            try {
                val deferredResults = sources.map { source ->
                    async(Dispatchers.IO) {
                        try {
                            source.fetchSearchManga(query, 1)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }

                val allResults = deferredResults.awaitAll().flatten()
                    .distinctBy {
                        it.url
                    }

                        _mangas.value = allResults

                        if (allResults.isEmpty()) {
                            _error.value =
                                "Ninguna de las ${sources.size} extensiones encontró: $query"
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