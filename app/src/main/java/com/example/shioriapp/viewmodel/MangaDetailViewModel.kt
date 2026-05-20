package com.example.shioriapp.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shioriapp.core.util.ExtensionLoader
import com.example.shioriapp.data.repository.LibraryManager
import com.example.shioriapp.domain.model.ChapterInfo
import com.example.shioriapp.domain.model.MangaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class DetailsState(
    val manga: MangaInfo? = null,
    val chapters: List<ChapterInfo> = emptyList(),
    val isLoading: Boolean = false
)

class MangaDetailViewModel : ViewModel() {
    private val _state = MutableStateFlow(DetailsState())
    val state: StateFlow<DetailsState> = _state.asStateFlow()

    fun loadMangaDetails(context: Context, url: String, sourceName: String, title: String) {
        val decodedSource = java.net.URLDecoder.decode(sourceName, "UTF-8")

        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {

            // ── 1. INTENTO DE CARGA INSTANTÁNEA (CACHÉ LOCAL NATIVO) ────────────
            val hash = url.hashCode()
            val capsFile = File(context.cacheDir, "${hash}_caps.json")
            val mangaFile = File(context.cacheDir, "${hash}_manga.json")

            try {
                if (capsFile.exists() && mangaFile.exists()) {
                    Log.d("SHIORI_APP", "🚀 Cargando desde caché instantáneo...")

                    // Leer información del Manga
                    val mObj = JSONObject(mangaFile.readText())
                    val cachedManga = MangaInfo(
                        title = mObj.optString("title", title),
                        url = mObj.optString("url", url),
                        coverUrl = mObj.optString("coverUrl", ""),
                        description = mObj.optString("description", ""),
                        author = mObj.optString("author", ""),
                        status = mObj.optInt("status", 0),
                        genres = mObj.optString("genres", ""),
                        sourceName = mObj.optString("sourceName", sourceName)
                    )

                    // Leer lista de Capítulos (🔥 CORREGIDO: Solo name y url)
                    val cArray = JSONArray(capsFile.readText())
                    val cachedCaps = mutableListOf<ChapterInfo>()
                    for (i in 0 until cArray.length()) {
                        val cObj = cArray.getJSONObject(i)
                        cachedCaps.add(
                            ChapterInfo(
                                name = cObj.getString("name"),
                                url = cObj.getString("url")
                            )
                        )
                    }

                    // Mandar a la UI inmediatamente para MATAR la pantalla negra
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(
                            manga = cachedManga,
                            chapters = cachedCaps,
                            isLoading = false // El Auto-Reanudar saltará de inmediato aquí
                        ) }
                    }
                }
            } catch (e: Exception) {
                Log.e("SHIORI_APP", "Error leyendo el caché rápido: ${e.message}")
            }

            // ── 2. ACTUALIZACIÓN SILENCIOSA EN LA RED (INTERNET) ────────────────
            try {
                val source = ExtensionLoader.getSource(decodedSource)
                if (source == null) {
                    _state.update { it.copy(isLoading = false) }
                    return@launch
                }

                val baseManga = MangaInfo(title = title, url = url, coverUrl = _state.value.manga?.coverUrl ?: "")
                val detailedManga = source.fetchMangaDetails(baseManga)

                val mangaFinal = if (detailedManga.title.isBlank() || detailedManga.title == "Manga") {
                    detailedManga.copy(title = title)
                } else {
                    detailedManga
                }

                val chapterList = source.fetchChapterList(mangaFinal)

                // ── 3. GUARDAR EL NUEVO RESULTADO EN CACHÉ ──────────────────────
                // Escribir archivo del Manga
                val mObj = JSONObject().apply {
                    put("title", mangaFinal.title)
                    put("url", mangaFinal.url)
                    put("coverUrl", mangaFinal.coverUrl)
                    put("description", mangaFinal.description)
                    put("author", mangaFinal.author)
                    put("status", mangaFinal.status)
                    put("genres", mangaFinal.genres)
                    put("sourceName", mangaFinal.sourceName)
                }
                mangaFile.writeText(mObj.toString())

                val cArray = JSONArray()
                chapterList.forEach { cap ->
                    val cObj = JSONObject().apply {
                        put("name", cap.name)
                        put("url", cap.url)
                    }
                    cArray.put(cObj)
                }
                capsFile.writeText(cArray.toString())

                LibraryManager.updateTotalChapters(context, url, chapterList.size)

                _state.update { it.copy(
                    manga = mangaFinal,
                    chapters = chapterList,
                    isLoading = false
                ) }

            } catch (e: Exception) {
                Log.e("SHIORI_APP", "Error cargando internet: ${e.message}")
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}