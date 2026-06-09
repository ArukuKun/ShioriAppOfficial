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
import kotlin.random.Random

data class ExploreState(
    val allMangas: List<MangaInfo> = emptyList(),
    val displayMangas: List<MangaInfo> = emptyList(),
    val categories: List<String> = listOf("Todo", "Acción", "Romance", "Comedia", "Fantasía", "Drama", "Sci-Fi"),
    val selectedCategory: String = "Todo",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val page: Int = 1,
    val isLastPage: Boolean = false,

    val isShowingSources: Boolean = false,
    val availableSources: List<String> = emptyList(),
    val selectedExtension: String? = null,
    val extensionMangas: List<MangaInfo> = emptyList(),
    val isExtensionLoading: Boolean = false,
    val isExtensionLoadingMore: Boolean = false,
    val extensionPage: Int = 1,
    val isExtensionLastPage: Boolean = false,
    val extensionTab: Int = 0
)

class ExploreViewModel : ViewModel() {
    private val _state = MutableStateFlow(ExploreState())
    val state: StateFlow<ExploreState> = _state

    private val sessionSeed = Random.nextLong()

    init {
        loadAvailableSources()
        fetchMangas(isInitial = true)
    }

    private fun loadAvailableSources() {
        viewModelScope.launch(Dispatchers.IO) {
            val sources = ExtensionLoader.getAvailableSources()
            _state.update { it.copy(availableSources = sources) }
        }
    }

    fun toggleSourcesView(showSources: Boolean) {
        _state.update { it.copy(isShowingSources = showSources) }
    }

    private fun fetchMangas(query: String = "", isInitial: Boolean = false) {
        _state.update { it.copy(isLoading = true, page = 1, isLastPage = false) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentSources = ExtensionLoader.getAvailableSources()
                val deferredRequests = currentSources.map { name ->
                    async {
                        ExtensionLoader.getSource(name)?.fetchSearchManga(query, 1) ?: emptyList()
                    }
                }
                val results = deferredRequests.awaitAll()
                val interleavedList = mutableListOf<MangaInfo>()
                val maxItems = results.maxOfOrNull { it.size } ?: 0
                for (i in 0 until maxItems) {
                    for (mangaList in results) {
                        if (i < mangaList.size) interleavedList.add(mangaList[i])
                    }
                }
                val shuffledInitialList = interleavedList.shuffled(Random(sessionSeed))
                _state.update { currentState ->
                    currentState.copy(
                        allMangas = if (isInitial) shuffledInitialList else currentState.allMangas,
                        displayMangas = shuffledInitialList,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setCategory(category: String) {
        if (_state.value.selectedCategory == category) return
        _state.update { it.copy(selectedCategory = category, page = 1) }
        if (category == "Todo") {
            _state.update { it.copy(displayMangas = it.allMangas) }
        } else {
            fetchMangas(query = category)
        }
    }

    fun loadMoreMangas() {
        if (state.value.isLoadingMore || state.value.isLastPage) return
        _state.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val nextPage = state.value.page + 1
                val currentQuery =
                    if (state.value.selectedCategory == "Todo") "" else state.value.selectedCategory
                val currentSources = ExtensionLoader.getAvailableSources()
                val deferredRequests = currentSources.map { name ->
                    async {
                        ExtensionLoader.getSource(name)?.fetchSearchManga(currentQuery, nextPage)
                            ?: emptyList()
                    }
                }
                val results = deferredRequests.awaitAll()
                val interleavedList = mutableListOf<MangaInfo>()
                val maxItems = results.maxOfOrNull { it.size } ?: 0
                for (i in 0 until maxItems) {
                    for (mangaList in results) {
                        if (i < mangaList.size) interleavedList.add(mangaList[i])
                    }
                }
                val hasMore = interleavedList.isNotEmpty()
                val shuffledNewPage = interleavedList.shuffled(Random(sessionSeed + nextPage))
                _state.update { currentState ->
                    currentState.copy(
                        isLoadingMore = false, page = nextPage, isLastPage = !hasMore,
                        allMangas = if (currentQuery.isEmpty()) currentState.allMangas + shuffledNewPage else currentState.allMangas,
                        displayMangas = currentState.displayMangas + shuffledNewPage
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    // --- 🔥 LÓGICA DE CATÁLOGO INMERSIVO PAGINADO ---

    fun openExtensionCatalog(sourceName: String) {
        _state.update {
            it.copy(
                selectedExtension = sourceName,
                extensionMangas = emptyList(),
                isExtensionLoading = true,
                extensionPage = 1,
                isExtensionLastPage = false,
                extensionTab = 0
            )
        }
        loadExtensionPage(sourceName, page = 1, tabIndex = 0)
    }

    fun closeExtensionCatalog() {
        _state.update { it.copy(selectedExtension = null, extensionMangas = emptyList()) }
    }

    fun setExtensionTab(tabIndex: Int) {
        if (state.value.extensionTab == tabIndex) return
        _state.update {
            it.copy(
                extensionTab = tabIndex,
                extensionMangas = emptyList(),
                isExtensionLoading = true,
                extensionPage = 1,
                isExtensionLastPage = false
            )
        }
        loadExtensionPage(state.value.selectedExtension!!, page = 1, tabIndex = tabIndex)
    }

    private fun loadExtensionPage(sourceName: String, page: Int, tabIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val source =
                    ExtensionLoader.getSource(sourceName) ?: throw Exception("Fuente no encontrada")

                val results = when (tabIndex) {
                    0 -> source.fetchPopularManga(page)
                    1 -> source.fetchLatestUpdates(page)
                    else -> source.fetchSearchManga("", page)
                }

                // Protegemos contra duplicados
                val distinctResults = results.distinctBy { it.url }

                _state.update {
                    it.copy(
                        extensionMangas = distinctResults,
                        isExtensionLoading = false,
                        isExtensionLastPage = distinctResults.isEmpty()
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("SHIORI_VM", "Error carga catálogo: ${e.message}")
                _state.update { it.copy(isExtensionLoading = false, extensionMangas = emptyList()) }
            }
        }
    }

    fun loadMoreExtensionMangas() {
        if (state.value.isExtensionLoadingMore || state.value.isExtensionLastPage) return

        _state.update { it.copy(isExtensionLoadingMore = true) }
        val sourceName = state.value.selectedExtension!!
        val tabIndex = state.value.extensionTab
        val nextPage = state.value.extensionPage + 1

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val source =
                    ExtensionLoader.getSource(sourceName) ?: throw Exception("Fuente no encontrada")

                val newMangas = when (tabIndex) {
                    0 -> source.fetchPopularManga(nextPage)
                    1 -> source.fetchLatestUpdates(nextPage)
                    else -> source.fetchSearchManga("", nextPage)
                }

                val distinctNewMangas = newMangas.distinctBy { it.url }

                _state.update { currentState ->
                    currentState.copy(
                        isExtensionLoadingMore = false,
                        extensionPage = nextPage,
                        isExtensionLastPage = distinctNewMangas.isEmpty(),
                        extensionMangas = currentState.extensionMangas + distinctNewMangas
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("SHIORI_VM", "Error paginación catálogo: ${e.message}")
                _state.update { it.copy(isExtensionLoadingMore = false) }
            }
        }
    }
}