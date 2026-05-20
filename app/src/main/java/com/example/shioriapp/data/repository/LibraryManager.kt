package com.example.shioriapp.data.repository

import android.content.Context
import com.example.shioriapp.domain.model.MangaInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import android.util.Log

@Serializable
data class ReadingProgress(
    val lastChapterUrl: String,
    val lastPage: Int,
    val totalPages: Int = 0,
    val readChapters: Set<String> = emptySet(),
    val lastReadTime: Long = 0L,     // Para ordenar "Seguir Leyendo"
    val totalChapters: Int = 0       // Para saber si ya leímos todo
)

object LibraryManager {
    private val _library = MutableStateFlow<List<MangaInfo>>(emptyList())
    val library: StateFlow<List<MangaInfo>> = _library.asStateFlow()

    private val _progressMap = MutableStateFlow<Map<String, ReadingProgress>>(emptyMap())
    val progressMap: StateFlow<Map<String, ReadingProgress>> = _progressMap.asStateFlow()

    private val _isChapterSortDescending = MutableStateFlow(true)
    val isChapterSortDescending: StateFlow<Boolean> = _isChapterSortDescending.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences("shiori_library", Context.MODE_PRIVATE)

        val jsonLib = prefs.getString("library_data", "[]") ?: "[]"
        _library.value = try { Json.decodeFromString(jsonLib) } catch (e: Exception) { emptyList() }

        val jsonProg = prefs.getString("reading_progress", "{}") ?: "{}"
        _progressMap.value = try { Json.decodeFromString(jsonProg) } catch (e: Exception) { emptyMap() }

        // 🔥 LOG: Verificamos qué valor se carga al iniciar la app
        val isSortDesc = prefs.getBoolean("sort_descending", true)
        Log.d("SHIORI_APP", "LibraryManager(init): Orden guardado recuperado -> isDescending = $isSortDesc")
        _isChapterSortDescending.value = isSortDesc

        isInitialized = true
    }

    fun setChapterSortDescending(context: Context, isDescending: Boolean) {
        // 🔥 LOG: Verificamos cuándo y a qué valor se cambia
        Log.d("SHIORI_APP", "LibraryManager(setSort): Cambiando orden a -> isDescending = $isDescending")

        _isChapterSortDescending.value = isDescending
        context.getSharedPreferences("shiori_library", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("sort_descending", isDescending)
            .apply()
    }

    fun saveProgress(
        context: Context,
        mangaUrl: String,
        chapterUrl: String,
        page: Int,
        totalPages: Int = 0,
        isFinished: Boolean = false,
        totalChapters: Int = 0 // 🔥 Nuevo parámetro opcional
    ) {
        val current = _progressMap.value.toMutableMap()
        val existing = current[mangaUrl] ?: ReadingProgress(chapterUrl, page)

        val newReadChapters = if (isFinished) {
            existing.readChapters + chapterUrl
        } else {
            existing.readChapters
        }

        current[mangaUrl] = existing.copy(
            lastChapterUrl = chapterUrl,
            lastPage = page,
            totalPages = totalPages,
            readChapters = newReadChapters,
            lastReadTime = System.currentTimeMillis(), // Guarda el tiempo exacto en que se leyó
            totalChapters = if (totalChapters > 0) totalChapters else existing.totalChapters
        )

        _progressMap.value = current
        persist(context, current)
    }

    // 🔥 NUEVA FUNCIÓN: Llama a esto cuando cargues la lista de capítulos para actualizar el total
    fun updateTotalChapters(context: Context, mangaUrl: String, total: Int) {
        val current = _progressMap.value.toMutableMap()
        val existing = current[mangaUrl]
        if (existing != null && existing.totalChapters != total) {
            current[mangaUrl] = existing.copy(totalChapters = total)
            _progressMap.value = current
            persist(context, current)
        }
    }

    fun toggleChapterRead(context: Context, mangaUrl: String, chapterUrl: String) {
        val current = _progressMap.value.toMutableMap()
        val existing = current[mangaUrl]
            ?: ReadingProgress(lastChapterUrl = chapterUrl, lastPage = 0)

        val newReadChapters = if (chapterUrl in existing.readChapters) {
            existing.readChapters - chapterUrl
        } else {
            existing.readChapters + chapterUrl
        }

        val newLastChapter = if (chapterUrl == existing.lastChapterUrl && chapterUrl !in newReadChapters) {
            newReadChapters.lastOrNull() ?: ""
        } else {
            existing.lastChapterUrl
        }

        current[mangaUrl] = existing.copy(
            readChapters = newReadChapters,
            lastChapterUrl = newLastChapter,
            lastReadTime = System.currentTimeMillis() // Actualiza tiempo manual
        )

        _progressMap.value = current
        persist(context, current)
    }

    fun markAllChaptersRead(context: Context, mangaUrl: String, chapterUrls: List<String>) {
        if (chapterUrls.isEmpty()) return

        val current = _progressMap.value.toMutableMap()
        val existing = current[mangaUrl]
            ?: ReadingProgress(lastChapterUrl = chapterUrls.first(), lastPage = 0)

        current[mangaUrl] = existing.copy(
            readChapters = existing.readChapters + chapterUrls.toSet(),
            lastChapterUrl = chapterUrls.first(),
            lastReadTime = System.currentTimeMillis()
        )

        _progressMap.value = current
        persist(context, current)
    }

    fun unmarkAllChapters(context: Context, mangaUrl: String) {
        val current = _progressMap.value.toMutableMap()
        current.remove(mangaUrl)
        _progressMap.value = current
        persist(context, current)
    }

    fun toggleManga(context: Context, manga: MangaInfo) {
        val current = _library.value.toMutableList()
        val exists = current.find { it.url == manga.url && it.sourceName == manga.sourceName }
        if (exists != null) current.remove(exists) else current.add(manga)
        _library.value = current
        context.getSharedPreferences("shiori_library", Context.MODE_PRIVATE)
            .edit().putString("library_data", Json.encodeToString(current)).apply()
    }

    private fun persist(context: Context, map: Map<String, ReadingProgress>) {
        context.getSharedPreferences("shiori_library", Context.MODE_PRIVATE)
            .edit().putString("reading_progress", Json.encodeToString(map)).apply()
    }
}