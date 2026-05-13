package com.example.shioriapp.domain.model

import androidx.compose.runtime.Immutable


@Immutable
data class ChapterInfo(
    val name: String,
    val url: String,
    val chapterNumber: Float = 0f,
    val dateUpload: Long = 0L,
    val scanlator: String? = null
)