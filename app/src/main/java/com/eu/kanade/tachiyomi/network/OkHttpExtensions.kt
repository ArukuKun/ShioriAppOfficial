package eu.kanade.tachiyomi.network

import okhttp3.Call
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import rx.Observable

fun Call.asObservableSuccess(): Observable<Response> {
    return Observable.fromCallable {
        val response = this.execute()
        if (!response.isSuccessful) throw Exception("HTTP Error ${response.code}")
        response
    }
}

fun Response.asJsoup(html: String? = null): Document {
    return Jsoup.parse(html ?: this.body?.string() ?: "", this.request.url.toString())
}