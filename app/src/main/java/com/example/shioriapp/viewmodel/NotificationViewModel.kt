package com.example.shioriapp.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.navigation.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationViewModel : ViewModel() {
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    fun addNotification(item: NotificationItem) {
        val currentList = _notifications.value.toMutableList()
        // Evitamos duplicar la misma notificación
        if (currentList.any { it.title == item.title && it.description == item.description }) return

        currentList.add(0, item)
        _notifications.value = currentList
    }

    fun clearAll() {
        _notifications.value = emptyList()
    }

    fun checkLibraryUpdates(libraryMangas: List<MangaInfo>) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        libraryMangas.forEach { manga ->
            val ultimoCapituloEnLaWeb = obtenerUltimoCapituloSimulado(manga.title)

            if (ultimoCapituloEnLaWeb != null) {

                val imagenPortada = manga.coverUrl

                val newUpdate = NotificationItem(
                    title = manga.title,
                    description = "¡Nuevo lanzamiento!: $ultimoCapituloEnLaWeb ya está disponible.",
                    icon = Icons.Default.Book,
                    color = Color(0xFF2196F3),
                    time = "Hoy a las $currentTime",
                    imageUrl = imagenPortada
                )
                addNotification(newUpdate)
            }
        }
    }

    private fun obtenerUltimoCapituloSimulado(mangaTitle: String): String? {
        return when (mangaTitle) {
            "Solo Leveling" -> "Capítulo 201"
            "One Piece" -> "Capítulo 1122"
            "Chainsaw Man" -> "Capítulo 168"
            else -> "Capítulo ${(10..100).random()}"
        }
    }
}