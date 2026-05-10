package com.example.shioriapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shioriapp.core.util.ExtensionLoader
import com.example.shioriapp.domain.model.MangaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExploreState(
    val allMangas: List<MangaInfo> = emptyList(),      // Aquí guardamos el "Todo" para no volver a descargarlo
    val displayMangas: List<MangaInfo> = emptyList(),  // Lo que se ve actualmente
    val categories: List<String> = listOf("Todo", "Acción", "Romance", "Comedia", "Fantasía", "Drama", "Sci-Fi"),
    val selectedCategory: String = "Todo",
    val isLoading: Boolean = false
)

class ExploreViewModel : ViewModel() {
    private val _state = MutableStateFlow(ExploreState())
    val state: StateFlow<ExploreState> = _state

    init {
        // Carga inicial (Todo)
        fetchMangas(isInitial = true)
    }

    // 🔥 Función unificada para traer contenido
    private fun fetchMangas(query: String = "", isInitial: Boolean = false) {
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sourceNames = ExtensionLoader.getAvailableSources()

                // Si no hay fuentes, esperamos un poco (por el retraso del sistema)
                if (sourceNames.isEmpty()) {
                    kotlinx.coroutines.delay(1000)
                }

                val currentSources = ExtensionLoader.getAvailableSources()

                val deferredRequests = currentSources.map { name ->
                    async {
                        val source = ExtensionLoader.getSource(name)
                        // Si query es "" trae populares. Si tiene texto, busca ese género.
                        source?.fetchSearchManga(query, 1) ?: emptyList()
                    }
                }

                val results = deferredRequests.awaitAll()

                // Intercalado (Mezcla de fuentes)
                val interleavedList = mutableListOf<MangaInfo>()
                val maxItems = results.maxOfOrNull { it.size } ?: 0
                for (i in 0 until maxItems) {
                    for (mangaList in results) {
                        if (i < mangaList.size) interleavedList.add(mangaList[i])
                    }
                }

                _state.update { currentState ->
                    currentState.copy(
                        // Si es la carga inicial ("Todo"), guardamos en el caché
                        allMangas = if (isInitial) interleavedList else currentState.allMangas,
                        displayMangas = interleavedList,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    // 🔥 Al presionar una categoría
    fun setCategory(category: String) {
        if (_state.value.selectedCategory == category) return // Evitar recargas inútiles

        _state.update { it.copy(selectedCategory = category) }

        if (category == "Todo") {
            // Si volvemos a "Todo", usamos lo que ya teníamos guardado (instantáneo)
            _state.update { it.copy(displayMangas = it.allMangas) }
        } else {
            // Si es una categoría nueva, disparamos búsqueda a internet
            fetchMangas(query = category)
        }
    }
}