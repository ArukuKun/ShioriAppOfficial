package com.example.shioriapp.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shioriapp.core.util.ExtensionLoader
import com.example.shioriapp.domain.model.ChapterInfo
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.domain.source.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

class MangaDetailViewModel : ViewModel() {

    private var source: Source? = null

    private val _manga = MutableStateFlow<MangaInfo?>(null)
    val manga = _manga.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterInfo>>(emptyList())
    val chapters = _chapters.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun init(context: Context, manga: MangaInfo) {
        _manga.value = manga
        val sources = ExtensionLoader.loadAllExtensions(context)
        source = sources.find { it.name == manga.sourceName }
        loadDetails(manga)
    }

    private fun loadDetails(manga: MangaInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val details = source?.fetchMangaDetails(manga) ?: manga
                _manga.value = details
                val chapterList = source?.fetchChapterList(details) ?: emptyList()
                _chapters.value = chapterList
            } catch (e: Exception) {
                _error.value = "Error al cargar detalles: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}