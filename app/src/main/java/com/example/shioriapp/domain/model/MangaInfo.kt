package com.example.shioriapp.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class MangaInfo(
    val url: String,
    val title: String,
    val artist: String = "",
    val author: String = "",
    val description: String = "",
    val genres: List<String> = emptyList(),
    val status: Int = 0,
    val coverUrl: String = "",
    val sourceName: String
)