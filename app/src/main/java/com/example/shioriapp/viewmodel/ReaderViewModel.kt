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

    fun initReader(initialChapter: ChapterInfo, chapters: List<ChapterInfo>, source: String) {
        allChapters = chapters
        sourceName = source
        loadedUrls.clear()

        _state.update { it.copy(isLoadingInitial = true, pages = emptyList()) }

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Carga el capítulo que seleccionaste (ej. el 5)
            loadChapterInternal(initialChapter, appendAtEnd = true)
            _state.update { it.copy(isLoadingInitial = false) }
            loadPrev()
            loadNext()
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

    fun loadNext() {
        if (_state.value.isLoadingNext || _state.value.pages.isEmpty()) return
        val lastChapter = _state.value.pages.last().chapter
        val idx = allChapters.indexOfFirst { it.url == lastChapter.url }
        if (idx > 0) {
            val next = allChapters[idx - 1]
            if (loadedUrls.contains(next.url)) return
            _state.update { it.copy(isLoadingNext = true) }
            viewModelScope.launch(Dispatchers.IO) { loadChapterInternal(next, true) }
        }
    }

    fun loadPrev() {
        if (_state.value.isLoadingPrev || _state.value.pages.isEmpty()) return
        val firstChapter = _state.value.pages.first().chapter
        val idx = allChapters.indexOfFirst { it.url == firstChapter.url }
        if (idx != -1 && idx < allChapters.size - 1) {
            val prev = allChapters[idx + 1]
            if (loadedUrls.contains(prev.url)) return
            _state.update { it.copy(isLoadingPrev = true) }
            viewModelScope.launch(Dispatchers.IO) { loadChapterInternal(prev, false) }
        }
    }

    fun clearReader() {
        loadedUrls.clear()
        _state.update { ReaderState() }
    }
}