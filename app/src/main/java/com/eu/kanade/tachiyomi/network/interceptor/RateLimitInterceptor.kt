package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

class RateLimitInterceptor(
    private val permits: Int,
    private val period: Long = 1,
    private val unit: TimeUnit = TimeUnit.SECONDS
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(chain.request())
    }
}

fun OkHttpClient.Builder.rateLimit(
    permits: Int,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS
): OkHttpClient.Builder = this.addNetworkInterceptor(RateLimitInterceptor(permits, period, unit))