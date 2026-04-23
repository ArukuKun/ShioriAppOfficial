package eu.kanade.tachiyomi.network

import okhttp3.OkHttpClient

class NetworkHelper {
    val client = OkHttpClient.Builder().build()
    val cloudflareClient = client
}