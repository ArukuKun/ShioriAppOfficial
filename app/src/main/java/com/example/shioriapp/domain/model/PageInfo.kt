package com.example.shioriapp.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class PageInfo(
    val index: Int,
    val url: String = "",
    val imageUrl: String? = null
)