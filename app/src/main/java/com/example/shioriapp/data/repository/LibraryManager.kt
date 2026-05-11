package com.example.shioriapp.data.repository

import android.content.Context
import com.example.shioriapp.domain.model.MangaInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Serializable
data class ReadingProgress(
    val lastChapterUrl: String,
    val lastPage: Int,
    val readChapters: Set<String> = emptySet()
)

object LibraryManager {
    private val _library = MutableStateFlow<List<MangaInfo>>(emptyList())
    val library: StateFlow<List<MangaInfo>> = _library.asStateFlow()

    private val _progressMap = MutableStateFlow<Map<String, ReadingProgress>>(emptyMap())
    val progressMap: StateFlow<Map<String, ReadingProgress>> = _progressMap.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences("shiori_library", Context.MODE_PRIVATE)

        // Cargar Biblioteca
        val jsonLib = prefs.getString("library_data", "[]") ?: "[]"
        _library.value = try { Json.decodeFromString(jsonLib) } catch (e: Exception) { emptyList() }

        // Cargar Progreso
        val jsonProg = prefs.getString("reading_progress", "{}") ?: "{}"
        _progressMap.value = try { Json.decodeFromString(jsonProg) } catch (e: Exception) { emptyMap() }

        isInitialized = true
    }

    fun saveProgress(context: Context, mangaUrl: String, chapterUrl: String, page: Int, isFinished: Boolean = false) {
        val current = _progressMap.value.toMutableMap()
        val existing = current[mangaUrl] ?: ReadingProgress(chapterUrl, page)

        val newReadChapters = if (isFinished) existing.readChapters + chapterUrl else existing.readChapters

        current[mangaUrl] = ReadingProgress(
            lastChapterUrl = chapterUrl,
            lastPage = page,
            readChapters = newReadChapters
        )

        _progressMap.value = current
        context.getSharedPreferences("shiori_library", Context.MODE_PRIVATE)
            .edit().putString("reading_progress", Json.encodeToString(current)).apply()
    }

    fun toggleManga(context: Context, manga: MangaInfo) {
        val current = _library.value.toMutableList()
        val exists = current.find { it.url == manga.url && it.sourceName == manga.sourceName }
        if (exists != null) current.remove(exists) else current.add(manga)
        _library.value = current
        context.getSharedPreferences("shiori_library", Context.MODE_PRIVATE)
            .edit().putString("library_data", Json.encodeToString(current)).apply()
    }
}