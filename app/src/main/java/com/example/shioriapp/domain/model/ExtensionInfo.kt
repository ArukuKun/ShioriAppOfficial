package com.example.shioriapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ExtensionInfo(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val nsfw: Int
)