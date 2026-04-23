package com.example.shioriapp.data.network

import com.example.shioriapp.domain.model.ExtensionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class ExtensionNetworkDataSource {

    private val client = OkHttpClient()
    private val defaultRepoUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json"
    private val jsonConfig = Json { ignoreUnknownKeys = true }

    suspend fun fetchExtensions(): List<ExtensionInfo> {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(defaultRepoUrl)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                jsonConfig.decodeFromString(responseBody)
            } else {
                emptyList()
            }
        }
    }
}