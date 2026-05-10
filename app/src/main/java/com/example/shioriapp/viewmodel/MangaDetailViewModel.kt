package com.example.shioriapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shioriapp.core.util.ExtensionLoader
import com.example.shioriapp.domain.model.MangaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailsState(
    val manga: MangaInfo? = null,
    val chapters: List<com.example.shioriapp.domain.model.ChapterInfo> = emptyList(),
    val isLoading: Boolean = false
)

class MangaDetailViewModel : ViewModel() {
    private val _state = MutableStateFlow(DetailsState())
    val state: StateFlow<DetailsState> = _state.asStateFlow()

    fun loadMangaDetails(url: String, sourceName: String, title: String) {
        // 👇 Decodifica "Bakaguya+Scanlation" → "Bakaguya Scanlation"
        val decodedSource = java.net.URLDecoder.decode(sourceName, "UTF-8")

        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("SHIORI_APP", "Buscando source: '$decodedSource'")
                val source = ExtensionLoader.getSource(decodedSource)
                Log.d("SHIORI_APP", "Source encontrado: $source")

                if (source == null) {
                    Log.e("SHIORI_APP", "Source es NULL para: '$decodedSource'")
                    _state.update { it.copy(isLoading = false) }
                    return@launch
                }

                val baseManga = MangaInfo(title = title, url = url, coverUrl = "")
                Log.d("SHIORI_APP", "Iniciando carga para: $title")

                val detailedManga = source.fetchMangaDetails(baseManga)
                Log.d("SHIORI_APP", "✅ fetchMangaDetails OK: ${detailedManga.title}")

                val mangaFinal = if (detailedManga.title.isBlank() || detailedManga.title == "Manga") {
                    detailedManga.copy(title = title)
                } else {
                    detailedManga
                }

                val chapterList = source.fetchChapterList(mangaFinal)
                Log.d("SHIORI_APP", "✅ fetchChapterList OK: ${chapterList.size} caps")

                _state.update { it.copy(
                    manga = mangaFinal,
                    chapters = chapterList,
                    isLoading = false
                ) }

            } catch (e: Exception) {
                Log.e("SHIORI_APP", "Error en ViewModel: ${e.message}")
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}