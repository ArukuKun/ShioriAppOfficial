package eu.kanade.tachiyomi.source.model

import android.net.Uri
import java.io.Serializable

// --- 1. MODELO DE MANGA ---

interface SManga : Serializable {
    var url: String
    var title: String
    var artist: String?
    var author: String?
    var description: String?
    var genre: String?
    var status: Int
    var thumbnail_url: String?
    var initialized: Boolean

    companion object {
        fun create(): SManga = SMangaImpl()
    }
}

class SMangaImpl : SManga {
    override var url: String = ""
    override var title: String = ""
    override var artist: String? = null
    override var author: String? = null
    override var description: String? = null
    override var genre: String? = null
    override var status: Int = 0
    override var thumbnail_url: String? = null
    override var initialized: Boolean = false
}

// --- 2. MODELO DE CAPÍTULO ---

interface SChapter : Serializable {
    var url: String
    var name: String
    var date_upload: Long
    var chapter_number: Float
    var scanlator: String?

    companion object {
        fun create(): SChapter = SChapterImpl()
    }
}

class SChapterImpl : SChapter {
    override var url: String = ""
    override var name: String = ""
    override var date_upload: Long = 0L
    override var chapter_number: Float = -1f
    override var scanlator: String? = null
}

data class MangasPage(
    val mangas: List<SManga>,
    val hasNextPage: Boolean
)

sealed class Filter<T>(val name: String, var state: T) {

    // Subclases obligatorias para extensiones como MangaDex
    open class Header(name: String) : Filter<Any>(name, 0)
    open class Separator(name: String = "") : Filter<Any>(name, 0)

    abstract class Select<V>(name: String, val values: Array<V>, state: Int = 0) :
        Filter<Int>(name, state)

    abstract class Text(name: String, state: String = "") : Filter<String>(name, state)

    abstract class CheckBox(name: String, state: Boolean = false) : Filter<Boolean>(name, state)

    abstract class Sort(name: String, val values: Array<String>, state: Selection? = null) :
        Filter<Sort.Selection?>(name, state) {
        class Selection(val index: Int, val ascending: Boolean)
    }

    abstract class TriState(name: String, state: Int = STATE_IGNORE) : Filter<Int>(name, state) {
        fun isIgnored() = state == STATE_IGNORE
        fun isIncluded() = state == STATE_INCLUDE
        fun isExcluded() = state == STATE_EXCLUDE

        companion object {
            const val STATE_IGNORE = 0
            const val STATE_INCLUDE = 1
            const val STATE_EXCLUDE = 2
        }
    }

    abstract class Group<V>(name: String, state: List<V>) : Filter<List<V>>(name, state)
}

class FilterList(val list: List<Filter<*>>) : List<Filter<*>> {
    constructor(vararg filters: Filter<*>) : this(filters.asList())

    override val size: Int get() = list.size
    override fun contains(element: Filter<*>): Boolean = list.contains(element)
    override fun containsAll(elements: Collection<Filter<*>>): Boolean = list.containsAll(elements)
    override fun get(index: Int): Filter<*> = list[index]
    override fun indexOf(element: Filter<*>): Int = list.indexOf(element)
    override fun isEmpty(): Boolean = list.isEmpty()
    override fun iterator(): Iterator<Filter<*>> = list.iterator()
    override fun lastIndexOf(element: Filter<*>): Int = list.lastIndexOf(element)
    override fun listIterator(): ListIterator<Filter<*>> = list.listIterator()
    override fun listIterator(index: Int): ListIterator<Filter<*>> = list.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<Filter<*>> = list.subList(fromIndex, toIndex)
}

// 🔥 AQUÍ ESTÁ LA MAGIA: Ahora es una "open class", justo lo que la extensión necesita
open class Page(
    val index: Int,
    val url: String = "",
    var imageUrl: String? = null,
    @Transient var uri: Uri? = null
) {
    val number: Int
        get() = index + 1

    @Transient
    @Volatile
    var status: Int = 0 // 0 = READY, 1 = LOAD_PAGE, 2 = DOWNLOAD_IMAGE, etc.
}