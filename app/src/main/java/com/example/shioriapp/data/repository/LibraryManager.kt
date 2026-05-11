package com.example.shioriapp.data.repository

import android.content.Context
import com.example.shioriapp.domain.model.MangaInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object LibraryManager {
    private val _library = MutableStateFlow<List<MangaInfo>>(emptyList())
    val library: StateFlow<List<MangaInfo>> = _library.asStateFlow()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences("shiori_library", Context.MODE_PRIVATE)
        val json = prefs.getString("library_data", "[]") ?: "[]"
        try {
            _library.value = Json.decodeFromString(json)
        } catch (e: Exception) {
            _library.value = emptyList()
        }
        isInitialized = true
    }

    fun toggleManga(context: Context, manga: MangaInfo) {
        val current = _library.value.toMutableList()
        val exists = current.find { it.url == manga.url && it.sourceName == manga.sourceName }
        if (exists != null) {
            current.remove(exists)
        } else {
            current.add(manga)
        }
        _library.value = current
        val prefs = context.getSharedPreferences("shiori_library", Context.MODE_PRIVATE)
        prefs.edit().putString("library_data", Json.encodeToString(current)).apply()
    }
}