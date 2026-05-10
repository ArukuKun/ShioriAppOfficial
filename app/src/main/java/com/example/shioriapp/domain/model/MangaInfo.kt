package com.example.shioriapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MangaInfo(
    val title: String,
    val url: String,
    val coverUrl: String,
    val author: String = "",
    val description: String = "",
    val status: Int = 0,
    val sourceName: String = "",
    // 🔥 EL CAMPO NUEVO PARA LOS FILTROS
    val genres: String = ""
)