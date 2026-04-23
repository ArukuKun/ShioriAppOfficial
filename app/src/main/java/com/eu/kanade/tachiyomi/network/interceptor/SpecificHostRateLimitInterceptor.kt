package eu.kanade.tachiyomi.network.interceptor

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

fun OkHttpClient.Builder.rateLimitHost(
    url: HttpUrl,
    permits: Int,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS
): OkHttpClient.Builder {
    return this.addNetworkInterceptor(RateLimitInterceptor(permits, period, unit))
}

fun OkHttpClient.Builder.rateLimitHost(
    host: String,
    permits: Int,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS
): OkHttpClient.Builder {
    return this.addNetworkInterceptor(RateLimitInterceptor(permits, period, unit))
}