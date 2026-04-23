package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import rx.Observable

abstract class HttpSource : CatalogueSource {

    // Generador de ID único para que MangaDex pueda guardar sus preferencias
    override val id: Long
        get() = (name + lang).hashCode().toLong()

    open val versionId = 1

    // Herramienta de red oficial para saltar Cloudflare y peticiones
    open val network = NetworkHelper()

    open val client: OkHttpClient
        get() = network.client

    open fun headersBuilder() = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
    open val headers: Headers by lazy { headersBuilder().build() }

    open fun SManga.setUrlWithoutDomain(orig: String) {
        this.url = getUrlWithoutDomain(orig)
    }

    open fun eu.kanade.tachiyomi.source.model.SChapter.setUrlWithoutDomain(orig: String) {
        this.url = getUrlWithoutDomain(orig)
    }

    private fun getUrlWithoutDomain(orig: String): String {
        return try {
            val uri = java.net.URI(orig)
            var out = uri.path ?: ""
            if (uri.query != null) out += "?" + uri.query
            if (uri.fragment != null) out += "#" + uri.fragment
            out
        } catch (e: Exception) {
            orig
        }
    }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return Observable.fromCallable {
            val request = searchMangaRequest(page, query, filters)
            android.util.Log.d("ShioriApp", "📡 1. Yendo a URL: ${request.url}")

            val response = client.newCall(request).execute()
            android.util.Log.d("ShioriApp", "📥 2. Respuesta del Servidor (HTTP): ${response.code}")

            val resultado = searchMangaParse(response)
            android.util.Log.d("ShioriApp", "📚 3. Mangas extraídos del HTML: ${resultado.mangas.size}")

            resultado
        }
    }

    abstract fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request
    abstract fun searchMangaParse(response: Response): MangasPage
}

// Lo separamos limpiamente para que no quede atrapado dentro de HttpSource
abstract class ParsedHttpSource : HttpSource() {
    override fun searchMangaParse(response: Response): MangasPage {
        val document = Jsoup.parse(response.body?.string() ?: "", response.request.url.toString())
        val mangas = document.select(searchMangaSelector()).map { element ->
            searchMangaFromElement(element)
        }
        val hasNextPage = searchMangaNextPageSelector()?.let { document.select(it).first() } != null
        return MangasPage(mangas, hasNextPage)
    }

    abstract fun searchMangaSelector(): String
    abstract fun searchMangaFromElement(element: Element): SManga
    abstract fun searchMangaNextPageSelector(): String?
}