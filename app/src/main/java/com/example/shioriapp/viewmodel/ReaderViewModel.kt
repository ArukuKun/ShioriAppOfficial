package com.example.shioriapp.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shioriapp.core.util.ExtensionLoader
import com.example.shioriapp.domain.model.ChapterInfo
import com.example.shioriapp.domain.model.PageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReaderViewModel : ViewModel() {

    private val _pages = MutableStateFlow<List<PageInfo>>(emptyList())
    val pages = _pages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadPages(context: Context, chapter: ChapterInfo, sourceName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val sources = ExtensionLoader.loadAllExtensions(context)
                val source = sources.find { it.name == sourceName }
                val pageList = source?.fetchPageList(chapter) ?: emptyList()
                _pages.value = pageList
            } catch (e: Exception) {
                _error.value = "Error al cargar páginas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}