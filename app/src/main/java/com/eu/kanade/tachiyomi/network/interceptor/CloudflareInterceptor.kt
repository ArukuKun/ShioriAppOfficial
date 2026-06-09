package com.eu.kanade.tachiyomi.network.interceptor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CloudflareInterceptor(private val context: Context) : Interceptor {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        // Verificamos si Cloudflare bloqueó la petición
        if (response.code != 403 && response.code != 503) {
            return response
        }

        val server = response.header("Server")?.lowercase() ?: ""
        if (!server.contains("cloudflare")) {
            return response
        }

        response.close()
        android.util.Log.e("SHIORI_CLOUDFLARE", "Iniciando bypass invisible (Estilo Mihon)...")

        val latch = CountDownLatch(1)
        var success = false

        mainHandler.post {
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true

            // 🔥 CRÍTICO: El WebView usa EXACTAMENTE el mismo User-Agent que OkHttp
            webView.settings.userAgentString = originalRequest.header("User-Agent")

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptThirdPartyCookies(webView, true)

            var isChecking = true

            val checker = object : Runnable {
                override fun run() {
                    if (!isChecking) return

                    val cookies = cookieManager.getCookie(originalRequest.url.toString()) ?: ""

                    if (cookies.contains("cf_clearance")) {
                        success = true
                        isChecking = false
                        cookieManager.flush()
                        webView.destroy()
                        latch.countDown()
                    } else {
                        mainHandler.postDelayed(this, 1000)
                    }
                }
            }

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    mainHandler.post(checker)
                }
            }

            webView.loadUrl(originalRequest.url.toString())

            // Límite de 15 segundos para buscar la galleta
            mainHandler.postDelayed({
                if (isChecking) {
                    isChecking = false
                    webView.destroy()
                    latch.countDown()
                }
            }, 15000)
        }

        latch.await(16, TimeUnit.SECONDS)

        if (success) {
            android.util.Log.e("SHIORI_CLOUDFLARE", "✅ Bypass exitoso. Reintentando...")
            // Como el AndroidCookieJar está vinculado a OkHttp, al reenviar la petición original,
            // OkHttp absorberá la nueva galleta cf_clearance automáticamente.
            return chain.proceed(originalRequest)
        } else {
            throw IOException("Error al saltar Cloudflare. La galleta no se generó.")
        }
    }
}