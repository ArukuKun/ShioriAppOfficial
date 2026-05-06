package eu.kanade.tachiyomi.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class NetworkHelper {

    private val userAgentInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            // Le decimos al servidor web: "Hola, soy un usuario normal usando Google Chrome en Windows 10"
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .build()
        chain.proceed(requestWithUserAgent)
    }

    val client = OkHttpClient.Builder()
        .addInterceptor(userAgentInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val cloudflareClient = client
}