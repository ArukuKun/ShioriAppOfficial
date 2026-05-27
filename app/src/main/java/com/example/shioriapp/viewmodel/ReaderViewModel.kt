package com.example.shioriapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shioriapp.core.util.ExtensionLoader
import com.example.shioriapp.domain.model.ChapterInfo
import com.example.shioriapp.domain.model.PageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderPage(
    val chapter: ChapterInfo,
    val page: PageInfo,
    val displayIndex: Int,
    val totalPages: Int,
    val uniqueId: String
)

data class ReaderState(
    val pages: List<ReaderPage> = emptyList(),
    val isLoadingInitial: Boolean = false,
    val isLoadingNext: Boolean = false,
    val isLoadingPrev: Boolean = false
)

class ReaderViewModel : ViewModel() {
    private val _state = MutableStateFlow(ReaderState())
    val state: StateFlow<ReaderState> = _state

    private var allChapters: List<ChapterInfo> = emptyList()
    private var sourceName: String = ""
    private val loadedUrls = mutableSetOf<String>()

    // 🔥 Variable para saber si el manga va del 1 al 100 o del 100 al 1
    private var isDescending = true

    fun initReader(initialChapter: ChapterInfo, chapters: List<ChapterInfo>, source: String) {
        allChapters = chapters
        sourceName = source
        loadedUrls.clear()

        // 🧠 Detección inteligente del orden de los capítulos
        if (chapters.size >= 2) {
            val numRegex = Regex("\\d+(\\.\\d+)?")
            val firstNum = numRegex.find(chapters.first().name)?.value?.toDoubleOrNull() ?: 0.0
            val lastNum = numRegex.find(chapters.last().name)?.value?.toDoubleOrNull() ?: 0.0
            isDescending = firstNum > lastNum
        }

        _state.update { it.copy(isLoadingInitial = true, pages = emptyList()) }

        viewModelScope.launch(Dispatchers.IO) {
            // Solo carga el capítulo actual y deja que el usuario decida si subir o bajar
            loadChapterInternal(initialChapter, appendAtEnd = true)
            _state.update { it.copy(isLoadingInitial = false) }
        }
    }

    private suspend fun loadChapterInternal(chapter: ChapterInfo, appendAtEnd: Boolean) {
        if (loadedUrls.contains(chapter.url)) return
        loadedUrls.add(chapter.url)

        try {
            val source = ExtensionLoader.getSource(sourceName)
            val pageList = source?.fetchPageList(chapter) ?: emptyList()
            val newPages = pageList.mapIndexed { index, page ->
                ReaderPage(chapter, page, index + 1, pageList.size, "${chapter.url}_p${index}")
            }

            _state.update { s ->
                s.copy(
                    // Si cargas hacia arriba, pones las páginas ANTES de las actuales
                    pages = if (appendAtEnd) s.pages + newPages else newPages + s.pages,
                    isLoadingNext = false,
                    isLoadingPrev = false
                )
            }
        } catch (e: Exception) {
            loadedUrls.remove(chapter.url)
            _state.update { it.copy(isLoadingNext = false, isLoadingPrev = false) }
        }
    }

    fun loadPrev() {
        if (_state.value.isLoadingPrev || _state.value.pages.isEmpty()) return
        val firstChapter = _state.value.pages.first().chapter
        val idx = allChapters.indexOfFirst { it.url == firstChapter.url }
        if (idx == -1) return

        // 🔥 Lógica perfecta sin importar el orden del scraper
        val prevChapterIndex = if (isDescending) idx + 1 else idx - 1

        if (prevChapterIndex in allChapters.indices) {
            val prev = allChapters[prevChapterIndex]
            if (loadedUrls.contains(prev.url)) return
            _state.update { it.copy(isLoadingPrev = true) }
            viewModelScope.launch(Dispatchers.IO) { loadChapterInternal(prev, appendAtEnd = false) }
        }
    }

    fun loadNext() {
        if (_state.value.isLoadingNext || _state.value.pages.isEmpty()) return
        val lastChapter = _state.value.pages.last().chapter
        val idx = allChapters.indexOfFirst { it.url == lastChapter.url }
        if (idx == -1) return

        // 🔥 Lógica perfecta sin importar el orden del scraper
        val nextChapterIndex = if (isDescending) idx - 1 else idx + 1

        if (nextChapterIndex in allChapters.indices) {
            val next = allChapters[nextChapterIndex]
            if (loadedUrls.contains(next.url)) return
            _state.update { it.copy(isLoadingNext = true) }
            viewModelScope.launch(Dispatchers.IO) { loadChapterInternal(next, appendAtEnd = true) }
        }
    }

    fun clearReader() {
        loadedUrls.clear()
        _state.update { ReaderState() }
    }
}