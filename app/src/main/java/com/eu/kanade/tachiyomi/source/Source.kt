package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import rx.Observable

interface Source {
    val id: Long
    val name: String
    val lang: String
}

interface CatalogueSource : Source {
    fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage>
}