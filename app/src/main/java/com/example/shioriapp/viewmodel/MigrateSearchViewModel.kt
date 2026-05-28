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
import kotlinx.coroutines.launch

sealed class MigrateSearchState {
    object Idle : MigrateSearchState()
    object Loading : MigrateSearchState()
    data class Success(val results: List<MangaInfo>) : MigrateSearchState()
    data class Error(val message: String) : MigrateSearchState()
}

class MigrateSearchViewModel : ViewModel() {

    private val _state = MutableStateFlow<MigrateSearchState>(MigrateSearchState.Idle)
    val state: StateFlow<MigrateSearchState> = _state

    fun searchGlobal(query: String, originalSourceName: String) {
        // Si ya está buscando o la caja de texto está vacía, no hacemos nada
        if (_state.value is MigrateSearchState.Loading || query.isBlank()) return

        _state.value = MigrateSearchState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val availableSourceNames = ExtensionLoader.getAvailableSources()

                val sourcesToSearch = availableSourceNames.filter { it != originalSourceName }

                val deferredResults = sourcesToSearch.map { sourceName ->
                    async {
                        try {
                            val source = ExtensionLoader.getSource(sourceName)

                            source?.fetchSearchManga(query = query, page = 1) ?: emptyList()
                        } catch (e: Exception) {
                            emptyList<MangaInfo>()
                        }
                    }
                }

                val allResults = deferredResults.awaitAll().flatten()

                _state.value = MigrateSearchState.Success(allResults)

            } catch (e: Exception) {
                _state.value = MigrateSearchState.Error("Error al buscar alternativas: ${e.message}")
            }
        }
    }
}