package eu.kanade.tachiyomi.network

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import com.eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class NetworkHelper(val context: Context? = null) {

    // 🔥 Volvemos al User-Agent 100% nativo de tu dispositivo. Sin disfraces.
    private val defaultUserAgent by lazy {
        if (context != null) {
            WebSettings.getDefaultUserAgent(context)
        } else {
            "Mozilla/5.0"
        }
    }

    private val headersInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()

        // Usamos .header() (sobrescribe) en vez de addHeader
        val requestBuilder = originalRequest.newBuilder()
            .header("User-Agent", defaultUserAgent)

        if (originalRequest.url.toString().contains("admin-ajax.php") || originalRequest.url.toString().contains("ajax")) {
            requestBuilder.header("X-Requested-With", "XMLHttpRequest")
        }

        chain.proceed(requestBuilder.build())
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(AndroidCookieJar())
        .addInterceptor(headersInterceptor)
        .apply {
            if (context != null) {
                // Interceptor limpio
                addInterceptor(CloudflareInterceptor(context))
            }
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val cloudflareClient = client
}

class AndroidCookieJar : CookieJar {
    private val cookieManager = CookieManager.getInstance().also {
        it.setAcceptCookie(true)
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val urlString = url.toString()
        cookies.forEach { cookie ->
            cookieManager.setCookie(urlString, cookie.toString())
        }
        cookieManager.flush()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookiesString = cookieManager.getCookie(url.toString()) ?: return emptyList()
        return cookiesString.split(";").mapNotNull { cookieStr ->
            Cookie.parse(url, cookieStr.trim())
        }
    }
}