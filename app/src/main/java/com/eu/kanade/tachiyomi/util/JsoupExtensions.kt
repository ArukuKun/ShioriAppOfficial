package eu.kanade.tachiyomi.util

import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

fun Response.asJsoup(html: String? = null): Document {
    return Jsoup.parse(html ?: this.body?.string() ?: "", this.request.url.toString())
}