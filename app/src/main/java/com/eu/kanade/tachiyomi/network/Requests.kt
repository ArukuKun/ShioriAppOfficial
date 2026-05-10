package eu.kanade.tachiyomi.network

import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl // <-- ¡Este es el import clave que faltaba!
import okhttp3.Request
import okhttp3.RequestBody

val DEFAULT_HEADERS = Headers.Builder().build()
val DEFAULT_BODY: RequestBody = FormBody.Builder().build()
val DEFAULT_CACHE_CONTROL = CacheControl.Builder().build()

// ─── FUNCIONES GET ─────────────────────────────────────────

fun GET(url: String, headers: Headers? = null, cache: CacheControl? = null): Request {
    return Request.Builder()
        .url(url)
        .headers(headers ?: DEFAULT_HEADERS)
        .cacheControl(cache ?: DEFAULT_CACHE_CONTROL)
        .build()
}

// 🔥 Esta es la que pedía el escáner para las búsquedas
fun GET(url: HttpUrl, headers: Headers? = null, cache: CacheControl? = null): Request {
    return Request.Builder()
        .url(url)
        .headers(headers ?: DEFAULT_HEADERS)
        .cacheControl(cache ?: DEFAULT_CACHE_CONTROL)
        .build()
}

// ─── FUNCIONES POST ────────────────────────────────────────

fun POST(url: String, headers: Headers? = null, body: RequestBody? = null, cache: CacheControl? = null): Request {
    return Request.Builder()
        .url(url)
        .post(body ?: DEFAULT_BODY)
        .headers(headers ?: DEFAULT_HEADERS)
        .cacheControl(cache ?: DEFAULT_CACHE_CONTROL)
        .build()
}

fun POST(url: HttpUrl, headers: Headers? = null, body: RequestBody? = null, cache: CacheControl? = null): Request {
    return Request.Builder()
        .url(url)
        .post(body ?: DEFAULT_BODY)
        .headers(headers ?: DEFAULT_HEADERS)
        .cacheControl(cache ?: DEFAULT_CACHE_CONTROL)
        .build()
}

// ─── FUNCIONES PUT ─────────────────────────────────────────

fun PUT(url: String, headers: Headers? = null, body: RequestBody? = null, cache: CacheControl? = null): Request {
    return Request.Builder()
        .url(url)
        .put(body ?: DEFAULT_BODY)
        .headers(headers ?: DEFAULT_HEADERS)
        .cacheControl(cache ?: DEFAULT_CACHE_CONTROL)
        .build()
}

fun PUT(url: HttpUrl, headers: Headers? = null, body: RequestBody? = null, cache: CacheControl? = null): Request {
    return Request.Builder()
        .url(url)
        .put(body ?: DEFAULT_BODY)
        .headers(headers ?: DEFAULT_HEADERS)
        .cacheControl(cache ?: DEFAULT_CACHE_CONTROL)
        .build()
}

// ─── FUNCIONES DELETE ──────────────────────────────────────

fun DELETE(url: String, headers: Headers? = null, body: RequestBody? = null, cache: CacheControl? = null): Request {
    return Request.Builder()
        .url(url)
        .delete(body ?: DEFAULT_BODY)
        .headers(headers ?: DEFAULT_HEADERS)
        .cacheControl(cache ?: DEFAULT_CACHE_CONTROL)
        .build()
}

fun DELETE(url: HttpUrl, headers: Headers? = null, body: RequestBody? = null, cache: CacheControl? = null): Request {
    return Request.Builder()
        .url(url)
        .delete(body ?: DEFAULT_BODY)
        .headers(headers ?: DEFAULT_HEADERS)
        .cacheControl(cache ?: DEFAULT_CACHE_CONTROL)
        .build()
}