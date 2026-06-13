package com.example.shioriapp.core.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.shioriapp.domain.model.ChapterInfo
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.domain.model.PageInfo
import dalvik.system.PathClassLoader
import com.example.shioriapp.domain.source.Source
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import java.io.IOException

object ExtensionLoader {

    private var isInjektInitialized = false
    private val activeSources = mutableMapOf<String, Source>()

    fun getSource(sourceName: String): Source? {
        return activeSources[sourceName]
    }

    fun loadAllExtensions(context: Context): List<Source> {
        val sources = mutableListOf<Source>()
        try {
            val packageManager = context.packageManager
            val flags = PackageManager.GET_META_DATA

            val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
            } else {
                packageManager.getInstalledApplications(flags)
            }

            android.util.Log.d("SHIORI_LOADER", "🔍 Buscando extensiones en ${apps.size} aplicaciones instaladas...")
            var mangaCount = 0
            var animeCount = 0

            for (appInfo in apps) {
                val pkgName = appInfo.packageName

                val isExtension = pkgName.contains("eu.kanade.tachiyomi.extension") ||
                        pkgName.contains("eu.kanade.tachiyomi.animeextension") ||
                        pkgName.contains("aniyomi.extension") ||
                        pkgName.contains("keiyoushin.extension") ||
                        appInfo.metaData?.containsKey("tachiyomi.extension.class") == true ||
                        appInfo.metaData?.containsKey("tachiyomi.animeextension.class") == true

                if (isExtension) {
                    val loadedSources = loadExtensionList(context, pkgName)
                    sources.addAll(loadedSources)

                    if (pkgName.contains("anime") || pkgName.contains("aniyomi")) {
                        animeCount += loadedSources.size
                    } else {
                        mangaCount += loadedSources.size
                    }

                    // 🔥 GUARDAMOS LA EXTENSIÓN EN LA BÓVEDA AL INSTANTE
                    loadedSources.forEach { source ->
                        activeSources[source.name] = source
                    }
                }
            }

            android.util.Log.d("SHIORI_LOADER", "✅ Carga completa: $mangaCount de Manga y $animeCount de Anime. Total en Bóveda: ${activeSources.size}")

        } catch (e: Throwable) {
            showToast(context, "Error en el escáner: ${e.message}")
        }
        return sources
    }

    fun loadExtensionList(context: Context, pkgName: String): List<Source> {
        val packageManager = context.packageManager
        val generatedSources = mutableListOf<Source>()

        if (!isInjektInitialized) {
            try {
                Injekt.importModule(object : InjektModule {
                    override fun InjektRegistrar.registerInjectables() {
                        addSingleton(context.applicationContext as android.app.Application)
                        addSingleton(eu.kanade.tachiyomi.network.NetworkHelper(context.applicationContext))
                        addSingleton(
                            kotlinx.serialization.json.Json {
                                ignoreUnknownKeys = true
                                explicitNulls = false
                                encodeDefaults = true
                                isLenient = true
                            }
                        )
                    }
                })
                isInjektInitialized = true
            } catch (e: Exception) {
                android.util.Log.e("ShioriApp", "Error iniciando Injekt", e)
            }
        }

        try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(pkgName, PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                packageManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
            }

            var sourceClassName: String? = null

            if (appInfo.metaData != null) {
                for (key in appInfo.metaData.keySet()) {
                    if (key.contains("extension.class", ignoreCase = true)) {
                        sourceClassName = appInfo.metaData.getString(key)
                        break
                    }
                }
            }

            if (sourceClassName == null) {
                val parts = pkgName.split(".")
                if (parts.size >= 2) {
                    val lang = parts[parts.size - 2]
                    val name = parts.last()
                    val camelCaseName = name.replaceFirstChar { it.uppercase() }
                    sourceClassName = "$pkgName.$camelCaseName"
                    android.util.Log.w("ShioriApp", "Metadata nula. Intentando adivinar clase: $sourceClassName")
                }
            }

            if (sourceClassName == null) {
                android.util.Log.w("ShioriApp", "Imposible determinar la clase para $pkgName")
                return emptyList()
            }

            if (sourceClassName.startsWith(".")) {
                sourceClassName = pkgName + sourceClassName
            }

            val classLoader = PathClassLoader(
                appInfo.sourceDir,
                appInfo.nativeLibraryDir,
                context.classLoader
            )

            val sourceClass = Class.forName(sourceClassName, false, classLoader)
            // 🪄 REVERTIDO A NEWINSTANCE() PARA COMPATIBILIDAD CON EXTENSIONES
            val sourceInstance = sourceClass.newInstance()

            val isFactory = try {
                sourceClass.getMethod("createSources") != null
            } catch (e: NoSuchMethodException) {
                false
            }

            if (isFactory) {
                val createSourcesMethod = sourceClass.getMethod("createSources")
                val sources = createSourcesMethod.invoke(sourceInstance) as? List<*> ?: emptyList<Any>()

                for (source in sources) {
                    if (source != null) {
                        val adapter = SourceAdapter(source, pkgName, packageManager, appInfo)
                        generatedSources.add(adapter)
                        android.util.Log.d("ShioriApp", "🌍 Fábrica generó: ${adapter.name} (${adapter.lang})")
                    }
                }
            } else {
                val adapter = SourceAdapter(sourceInstance, pkgName, packageManager, appInfo)
                generatedSources.add(adapter)
                android.util.Log.d("ShioriApp", "📄 Extensión cargada: ${adapter.name} (${adapter.lang})")
            }

            return generatedSources

        } catch (e: Throwable) {
            android.util.Log.e("ShioriApp", "💀 ERROR AL CARGAR EXTENSIÓN: $pkgName", e)
            return emptyList()
        }
    }

    private fun showToast(context: Context, message: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun getAvailableSources(): List<String> {
        return activeSources.keys.toList()
    }
}

class SourceAdapter(
    private val extensionInstance: Any,
    private val pkgName: String,
    private val packageManager: PackageManager,
    private val appInfo: ApplicationInfo
) : Source {

    override val name: String
        get() = packageManager.getApplicationLabel(appInfo).toString()
            .removePrefix("Tachiyomi: ")
            .removePrefix("Aniyomi: ")
            .trim()

    override val lang: String
        get() = getPropertyValue("lang") as? String ?: "unknown"

    override val id: Long
        get() = getPropertyValue("id") as? Long ?: 0L

    private fun buildSManga(manga: MangaInfo): Any {
        val sMangaClass = extensionInstance.javaClass.classLoader!!
            .loadClass("eu.kanade.tachiyomi.source.model.SMangaImpl")

        val sManga = sMangaClass.getDeclaredConstructor().newInstance()

        sManga.javaClass.getMethod("setUrl", String::class.java).invoke(sManga, manga.url)
        sManga.javaClass.getMethod("setTitle", String::class.java).invoke(sManga, manga.title)
        sManga.javaClass.getMethod("setInitialized", Boolean::class.java).invoke(sManga, true)

        return sManga
    }

    private fun extractMangaInfo(result: Any, base: MangaInfo): MangaInfo {
        val rc = result.javaClass
        val newTitle  = try { rc.getMethod("getTitle").invoke(result) as? String } catch (e: Exception) { null }
        val newDesc   = try { rc.getMethod("getDescription").invoke(result) as? String } catch (e: Exception) { null }
        val newAuthor = try { rc.getMethod("getAuthor").invoke(result) as? String } catch (e: Exception) { null }
        val newCover  = try { rc.getMethod("getThumbnail_url").invoke(result) as? String } catch (e: Exception) { null }
        val newStatus = try { rc.getMethod("getStatus").invoke(result) as? Int } catch (e: Exception) { null }

        return base.copy(
            title       = if (!newTitle.isNullOrBlank() && newTitle != "Manga") newTitle else base.title,
            description = if (!newDesc.isNullOrBlank()) newDesc else base.description,
            author      = if (!newAuthor.isNullOrBlank()) newAuthor else base.author,
            coverUrl    = if (!newCover.isNullOrBlank()) newCover else base.coverUrl,
            status      = newStatus ?: base.status
        )
    }

    override suspend fun fetchSearchManga(query: String, page: Int): List<MangaInfo> {
        val TAG = "SHIORI_SEARCH"
        val mangaList = mutableListOf<MangaInfo>()

        try {
            val method = try {
                extensionInstance.javaClass.getMethod(
                    "fetchSearchManga", Int::class.java, String::class.java, eu.kanade.tachiyomi.source.model.FilterList::class.java
                )
            } catch (e: NoSuchMethodException) { null }

            if (method == null) return emptyList()

            val emptyFilters = eu.kanade.tachiyomi.source.model.FilterList(emptyList())
            val observable = method.invoke(extensionInstance, page, query, emptyFilters) ?: return emptyList()

            val blocking = observable.javaClass.getMethod("toBlocking").invoke(observable)
            val result = blocking.javaClass.getMethod("first").invoke(blocking) as? eu.kanade.tachiyomi.source.model.MangasPage ?: return emptyList()

            result.mangas.forEach { sManga ->
                mangaList.add(
                    MangaInfo(
                        title = sManga.title, url = sManga.url, coverUrl = sManga.thumbnail_url ?: "",
                        author = sManga.author ?: "", status = sManga.status, sourceName = this.name, genres = sManga.genre ?: ""
                    )
                )
            }
        } catch (e: Throwable) {
            android.util.Log.e(TAG, "💀 [${this.name}] Error GENERAL en búsqueda:", e)
        }
        return mangaList
    }

    override suspend fun fetchPopularManga(page: Int): List<MangaInfo> {
        val mangaList = mutableListOf<MangaInfo>()
        try {
            val method = extensionInstance.javaClass.getMethod("fetchPopularManga", Int::class.java)
            val observable = method.invoke(extensionInstance, page) ?: return emptyList()
            val blocking = observable.javaClass.getMethod("toBlocking").invoke(observable)
            val result = blocking.javaClass.getMethod("first").invoke(blocking) as? eu.kanade.tachiyomi.source.model.MangasPage ?: return emptyList()

            result.mangas.forEach { sManga ->
                mangaList.add(
                    MangaInfo(title = sManga.title, url = sManga.url, coverUrl = sManga.thumbnail_url ?: "",
                        author = sManga.author ?: "", status = sManga.status, sourceName = this.name, genres = sManga.genre ?: "")
                )
            }
        } catch (e: Exception) {}
        return mangaList
    }

    override suspend fun fetchLatestUpdates(page: Int): List<MangaInfo> {
        val mangaList = mutableListOf<MangaInfo>()
        try {
            val method = extensionInstance.javaClass.getMethod("fetchLatestUpdates", Int::class.java)
            val observable = method.invoke(extensionInstance, page) ?: return emptyList()
            val blocking = observable.javaClass.getMethod("toBlocking").invoke(observable)
            val result = blocking.javaClass.getMethod("first").invoke(blocking) as? eu.kanade.tachiyomi.source.model.MangasPage ?: return emptyList()

            result.mangas.forEach { sManga ->
                mangaList.add(
                    MangaInfo(title = sManga.title, url = sManga.url, coverUrl = sManga.thumbnail_url ?: "",
                        author = sManga.author ?: "", status = sManga.status, sourceName = this.name, genres = sManga.genre ?: "")
                )
            }
        } catch (e: Exception) {}
        return mangaList
    }

    private fun findAllMethodsByName(clazz: Class<*>, name: String): List<java.lang.reflect.Method> {
        val methods = mutableListOf<java.lang.reflect.Method>()
        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            current.declaredMethods.forEach {
                if (it.name == name) {
                    it.isAccessible = true
                    methods.add(it)
                }
            }
            current = current.superclass
        }
        return methods
    }

    override suspend fun fetchMangaDetails(manga: MangaInfo): MangaInfo {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val sMangaClass = extensionInstance.javaClass.classLoader!!.loadClass("eu.kanade.tachiyomi.source.model.SMangaImpl")
                val sManga = sMangaClass.getDeclaredConstructor().newInstance()
                sManga.javaClass.getMethod("setUrl", String::class.java).invoke(sManga, manga.url)
                sManga.javaClass.getMethod("setTitle", String::class.java).invoke(sManga, manga.title)

                val allMethods = extensionInstance.javaClass.methods
                var resultDetails: Any? = null

                // ── Intento A: API Moderna (Suspend) ──
                val getMethod = allMethods.firstOrNull { it.name == "getMangaDetails" && it.parameterCount == 2 }
                if (getMethod != null) {
                    try {
                        kotlinx.coroutines.suspendCancellableCoroutine<Any?> { continuation ->
                            try {
                                val res = getMethod.invoke(extensionInstance, sManga, continuation)
                                // 🔥 Mejora: Validar isActive para evitar crashes si el usuario canceló la acción rápido
                                if (res !== kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED && continuation.isActive) {
                                    continuation.resumeWith(Result.success(res))
                                }
                            } catch (e: Exception) {
                                if (continuation.isActive) continuation.resumeWith(Result.failure(e))
                            }
                        }
                        resultDetails = sManga
                    } catch (e: Exception) {}
                }

                val tempDescA = try { sManga.javaClass.getMethod("getDescription").invoke(sManga) as? String } catch (e: Exception) { null }

                // ── Intento B: API Antigua (RxJava) ──
                if (tempDescA.isNullOrBlank()) {
                    resultDetails = null
                    val fetchMethod = allMethods.firstOrNull { it.name == "fetchMangaDetails" && it.parameterCount == 1 }
                    if (fetchMethod != null) {
                        try {
                            val observable = fetchMethod.invoke(extensionInstance, sManga)
                            if (observable != null) {
                                val blocking = observable.javaClass.getMethod("toBlocking").invoke(observable)
                                resultDetails = blocking.javaClass.getMethod("first").invoke(blocking)
                            }
                        } catch (e: Exception) {}
                    }
                }

                val tempObjB = resultDetails ?: sManga
                val tempDescB = try { tempObjB.javaClass.getMethod("getDescription").invoke(tempObjB) as? String } catch (e: Exception) { null }

                // ── Intento C: MODO SUPERVIVENCIA HTTP ──
                if (tempDescB.isNullOrBlank()) {
                    val parseMethods = findAllMethodsByName(extensionInstance.javaClass, "mangaDetailsParse")

                    if (parseMethods.isNotEmpty()) {
                        val clientMethod = allMethods.firstOrNull { it.name == "getClient" }
                        val client = clientMethod?.invoke(extensionInstance) as? okhttp3.OkHttpClient

                        if (client != null) {
                            var request: okhttp3.Request? = null
                            val requestMethods = findAllMethodsByName(extensionInstance.javaClass, "mangaDetailsRequest")
                            val reqMethod = requestMethods.firstOrNull { it.parameterCount == 1 }

                            if (reqMethod != null) {
                                request = reqMethod.invoke(extensionInstance, sManga) as? okhttp3.Request
                            } else {
                                val baseUrlMethod = allMethods.firstOrNull { it.name == "getBaseUrl" }
                                val baseUrl = baseUrlMethod?.invoke(extensionInstance) as? String

                                if (baseUrl != null) {
                                    val fullUrl = if (manga.url.startsWith("http")) manga.url else baseUrl + manga.url
                                    val headersMethod = allMethods.firstOrNull { it.name == "getHeaders" }
                                    val headers = headersMethod?.invoke(extensionInstance) as? okhttp3.Headers

                                    val reqBuilder = okhttp3.Request.Builder().url(fullUrl).get()
                                    if (headers != null) reqBuilder.headers(headers)
                                    request = reqBuilder.build()
                                }
                            }

                            if (request != null) {
                                try {
                                    // 🔥 Mejora: Usar .use {} para cerrar la conexión y evitar memory leaks
                                    client.newCall(request).execute().use { response ->
                                        // 🔥 Mejora: Validar respuesta exitosa antes de parsear basura
                                        if (response.isSuccessful) {
                                            val bodyString = response.body?.string() ?: ""

                                            for (parseMethod in parseMethods) {
                                                try {
                                                    val paramType = parseMethod.parameterTypes.firstOrNull()?.name ?: ""
                                                    var tempResult: Any? = null

                                                    if (paramType.contains("Document")) {
                                                        val document = org.jsoup.Jsoup.parse(bodyString, request.url.toString())
                                                        tempResult = parseMethod.invoke(extensionInstance, document)
                                                    } else if (paramType.contains("Response")) {
                                                        val newBody = okhttp3.ResponseBody.create(response.body?.contentType(), bodyString)
                                                        val newResponse = response.newBuilder().body(newBody).build()
                                                        tempResult = parseMethod.invoke(extensionInstance, newResponse)
                                                    } else {
                                                        tempResult = parseMethod.invoke(extensionInstance, bodyString)
                                                    }

                                                    if (tempResult != null && tempResult.javaClass.name != "kotlin.Unit") {
                                                        resultDetails = tempResult
                                                        break
                                                    } else {
                                                        val checkDesc = try { sManga.javaClass.getMethod("getDescription").invoke(sManga) as? String } catch (e: Exception) { null }
                                                        if (!checkDesc.isNullOrBlank()) {
                                                            resultDetails = sManga
                                                            break
                                                        }
                                                    }
                                                } catch (e: Exception) {}
                                            }
                                        }
                                    }
                                } catch (e: IOException) {
                                    android.util.Log.e("SHIORI_DETAILS", "Network error: ${e.message}")
                                }
                            }
                        }
                    }
                }

                val finalObject = resultDetails ?: sManga
                return@withContext extractMangaInfo(finalObject, manga)

            } catch (e: Throwable) {
                return@withContext manga
            }
        }
    }

    override suspend fun fetchChapterList(manga: MangaInfo): List<ChapterInfo> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val TAG = "SHIORI_CHAPTERS"
            try {
                val sMangaClass = extensionInstance.javaClass.classLoader!!.loadClass("eu.kanade.tachiyomi.source.model.SMangaImpl")
                val sManga = sMangaClass.getDeclaredConstructor().newInstance()
                sManga.javaClass.getMethod("setUrl", String::class.java).invoke(sManga, manga.url)
                sManga.javaClass.getMethod("setTitle", String::class.java).invoke(sManga, manga.title)

                val allMethods = extensionInstance.javaClass.methods
                var resultList: List<*>? = null

                val hasGetChapterList = allMethods.any { it.name == "getChapterList" && it.parameterCount == 2 }
                val hasFetchChapterList = allMethods.any { it.name == "fetchChapterList" && it.parameterCount == 1 }

                // ── Intento A: API Moderna (Suspend) ──
                if (hasGetChapterList) {
                    val getMethod = allMethods.first { it.name == "getChapterList" && it.parameterCount == 2 }
                    try {
                        val res = kotlinx.coroutines.suspendCancellableCoroutine<Any?> { continuation ->
                            try {
                                val invokeRes = getMethod.invoke(extensionInstance, sManga, continuation)
                                if (invokeRes !== kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED && continuation.isActive) {
                                    continuation.resumeWith(Result.success(invokeRes))
                                }
                            } catch (e: Exception) {
                                if (continuation.isActive) continuation.resumeWith(Result.failure(e))
                            }
                        }
                        resultList = res as? List<*>
                    } catch (e: Exception) { }
                }

                // ── Intento B: API Antigua (RxJava) ──
                if (resultList.isNullOrEmpty() && hasFetchChapterList) {
                    val fetchMethod = allMethods.first { it.name == "fetchChapterList" && it.parameterCount == 1 }
                    try {
                        val observable = fetchMethod.invoke(extensionInstance, sManga)
                        if (observable != null) {
                            val blocking = observable.javaClass.getMethod("toBlocking").invoke(observable)
                            resultList = blocking.javaClass.getMethod("first").invoke(blocking) as? List<*>
                        }
                    } catch (e: Exception) { }
                }

                // ── Intento C: MODO SUPERVIVENCIA HTTP ──
                if (resultList.isNullOrEmpty()) {
                    val parseMethods = findAllMethodsByName(extensionInstance.javaClass, "chapterListParse")

                    if (parseMethods.isNotEmpty()) {
                        val clientMethod = allMethods.firstOrNull { it.name == "getClient" }
                        val client = clientMethod?.invoke(extensionInstance) as? okhttp3.OkHttpClient

                        if (client != null) {
                            var request: okhttp3.Request? = null
                            val requestMethods = findAllMethodsByName(extensionInstance.javaClass, "chapterListRequest")
                            val reqMethod = requestMethods.firstOrNull { it.parameterCount == 1 }

                            if (reqMethod != null) {
                                request = reqMethod.invoke(extensionInstance, sManga) as? okhttp3.Request
                            } else {
                                val baseUrlMethod = allMethods.firstOrNull { it.name == "getBaseUrl" }
                                val baseUrl = baseUrlMethod?.invoke(extensionInstance) as? String
                                if (baseUrl != null) {
                                    val fullUrl = if (manga.url.startsWith("http")) manga.url else baseUrl + manga.url
                                    val headersMethod = allMethods.firstOrNull { it.name == "getHeaders" }
                                    val headers = headersMethod?.invoke(extensionInstance) as? okhttp3.Headers
                                    val reqBuilder = okhttp3.Request.Builder().url(fullUrl).get()
                                    if (headers != null) reqBuilder.headers(headers)
                                    request = reqBuilder.build()
                                }
                            }

                            if (request != null) {
                                try {
                                    // 🔥 Mejora: Bloque .use para limpieza de recursos y evitar leaks
                                    client.newCall(request).execute().use { response ->
                                        // 🔥 Mejora: Evitar parseos fatales si el servidor devolvió un 500
                                        if (response.isSuccessful) {
                                            val bodyString = response.body?.string() ?: ""
                                            for (parseMethod in parseMethods) {
                                                try {
                                                    val paramType = parseMethod.parameterTypes.firstOrNull()?.name ?: ""
                                                    var tempResult: Any? = null
                                                    if (paramType.contains("Response")) {
                                                        val newBody = okhttp3.ResponseBody.create(response.body?.contentType(), bodyString)
                                                        val newResponse = response.newBuilder().body(newBody).build()
                                                        tempResult = parseMethod.invoke(extensionInstance, newResponse)
                                                    } else if (paramType.contains("Document")) {
                                                        val document = org.jsoup.Jsoup.parse(bodyString, request.url.toString())
                                                        tempResult = parseMethod.invoke(extensionInstance, document)
                                                    } else {
                                                        tempResult = parseMethod.invoke(extensionInstance, bodyString)
                                                    }

                                                    if (tempResult is List<*>) {
                                                        resultList = tempResult
                                                        break
                                                    }
                                                } catch (e: Exception) { }
                                            }
                                        } else {
                                            android.util.Log.e(TAG, "❌ HTTP Error en capítulos: ${response.code}")
                                        }
                                    }
                                } catch (e: IOException) {
                                    android.util.Log.e(TAG, "Network error: ${e.message}")
                                }
                            }
                        }
                    }
                }

                val finalChapterList = mutableListOf<ChapterInfo>()
                val safeList = resultList

                // 🔥 AQUÍ ESTÁ LA CORRECCIÓN CLAVE: Un solo bucle limpio sobre safeList
                if (!safeList.isNullOrEmpty()) {
                    val firstNonNull = safeList.firstOrNull { it != null }
                    if (firstNonNull != null) {
                        val c = firstNonNull.javaClass
                        // Cache de métodos
                        val getNameMethod = try { c.getMethod("getName") } catch (e: Exception) { null }
                        val getUrlMethod = try { c.getMethod("getUrl") } catch (e: Exception) { null }
                        val getChapterNumMethod = try { c.getMethod("getChapter_number") } catch (e: Exception) { null }
                        val getDateUploadMethod = try { c.getMethod("getDate_upload") } catch (e: Exception) { null }
                        val getScanlatorMethod = try { c.getMethod("getScanlator") } catch (e: Exception) { null }

                        for (item in safeList) {
                            if (item == null) continue
                            try {
                                finalChapterList.add(
                                    ChapterInfo(
                                        name = getNameMethod?.invoke(item) as? String ?: "Capítulo sin nombre",
                                        url = getUrlMethod?.invoke(item) as? String ?: "",
                                        chapterNumber = getChapterNumMethod?.invoke(item) as? Float ?: -1f,
                                        dateUpload = getDateUploadMethod?.invoke(item) as? Long ?: 0L,
                                        scanlator = getScanlatorMethod?.invoke(item) as? String
                                    )
                                )
                            } catch (e: Exception) {
                                android.util.Log.e(TAG, "Error mapeando capítulo: ${e.message}")
                            }
                        }
                    }
                }

                return@withContext finalChapterList

            } catch (e: Throwable) {
                return@withContext emptyList()
            }
        }
    }

    override suspend fun fetchPageList(chapter: ChapterInfo): List<PageInfo> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val TAG = "SHIORI_PAGES"
            try {
                val sChapterClass = extensionInstance.javaClass.classLoader!!.loadClass("eu.kanade.tachiyomi.source.model.SChapterImpl")
                val sChapter = sChapterClass.getDeclaredConstructor().newInstance()
                sChapter.javaClass.getMethod("setUrl", String::class.java).invoke(sChapter, chapter.url)
                sChapter.javaClass.getMethod("setName", String::class.java).invoke(sChapter, chapter.name)

                val allMethods = extensionInstance.javaClass.methods
                var resultList: List<*>? = null

                val hasGetPageList = allMethods.any { it.name == "getPageList" && it.parameterCount == 2 }
                val hasFetchPageList = allMethods.any { it.name == "fetchPageList" && it.parameterCount == 1 }

                // ── Intento A: API Moderna (Suspend) ──
                if (hasGetPageList) {
                    val getMethod = allMethods.first { it.name == "getPageList" && it.parameterCount == 2 }
                    try {
                        val res = kotlinx.coroutines.suspendCancellableCoroutine<Any?> { continuation ->
                            try {
                                val invokeRes = getMethod.invoke(extensionInstance, sChapter, continuation)
                                if (invokeRes !== kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED && continuation.isActive) {
                                    continuation.resumeWith(Result.success(invokeRes))
                                }
                            } catch (e: Exception) {
                                if (continuation.isActive) continuation.resumeWith(Result.failure(e))
                            }
                        }
                        resultList = res as? List<*>
                    } catch (e: Exception) { }
                }

                // ── Intento B: API Antigua (RxJava) ──
                if (resultList.isNullOrEmpty() && hasFetchPageList) {
                    val fetchMethod = allMethods.first { it.name == "fetchPageList" && it.parameterCount == 1 }
                    try {
                        val observable = fetchMethod.invoke(extensionInstance, sChapter)
                        if (observable != null) {
                            val blocking = observable.javaClass.getMethod("toBlocking").invoke(observable)
                            resultList = blocking.javaClass.getMethod("first").invoke(blocking) as? List<*>
                        }
                    } catch (e: Exception) { }
                }

                // 🔥 ── Intento C: MODO SUPERVIVENCIA HTTP ──
                if (resultList.isNullOrEmpty()) {
                    val parseMethods = findAllMethodsByName(extensionInstance.javaClass, "pageListParse")

                    if (parseMethods.isNotEmpty()) {
                        val clientMethod = allMethods.firstOrNull { it.name == "getClient" }
                        val client = clientMethod?.invoke(extensionInstance) as? okhttp3.OkHttpClient

                        if (client != null) {
                            var request: okhttp3.Request? = null
                            val requestMethods = findAllMethodsByName(extensionInstance.javaClass, "pageListRequest")
                            val reqMethod = requestMethods.firstOrNull { it.parameterCount == 1 }

                            if (reqMethod != null) {
                                request = reqMethod.invoke(extensionInstance, sChapter) as? okhttp3.Request
                            } else {
                                val baseUrlMethod = allMethods.firstOrNull { it.name == "getBaseUrl" }
                                val baseUrl = baseUrlMethod?.invoke(extensionInstance) as? String
                                if (baseUrl != null) {
                                    val fullUrl = if (chapter.url.startsWith("http")) chapter.url else baseUrl + chapter.url
                                    val headersMethod = allMethods.firstOrNull { it.name == "getHeaders" }
                                    val headers = headersMethod?.invoke(extensionInstance) as? okhttp3.Headers
                                    val reqBuilder = okhttp3.Request.Builder().url(fullUrl).get()
                                    if (headers != null) reqBuilder.headers(headers)
                                    request = reqBuilder.build()
                                }
                            }

                            if (request != null) {
                                try {
                                    // 🔥 Mejora: Bloque .use y validación isSuccessful
                                    client.newCall(request).execute().use { response ->
                                        if (response.isSuccessful) {
                                            val bodyString = response.body?.string() ?: ""
                                            for (parseMethod in parseMethods) {
                                                try {
                                                    var tempResult: Any? = null
                                                    val paramType = parseMethod.parameterTypes.firstOrNull()?.name ?: ""

                                                    if (paramType.contains("Response")) {
                                                        val newBody = okhttp3.ResponseBody.create(response.body?.contentType(), bodyString)
                                                        val newResponse = response.newBuilder().body(newBody).build()
                                                        tempResult = parseMethod.invoke(extensionInstance, newResponse)
                                                    } else if (paramType.contains("Document")) {
                                                        val document = org.jsoup.Jsoup.parse(bodyString, request.url.toString())
                                                        tempResult = parseMethod.invoke(extensionInstance, document)
                                                    }
                                                    if (tempResult is List<*>) {
                                                        resultList = tempResult
                                                        break
                                                    }
                                                } catch (e: Exception) { }
                                            }
                                        } else {
                                            android.util.Log.e(TAG, "❌ HTTP Error en páginas: ${response.code}")
                                        }
                                    }
                                } catch (e: IOException) {
                                    android.util.Log.e(TAG, "Network error: ${e.message}")
                                }
                            }
                        }
                    }
                }

                val finalPages = mutableListOf<PageInfo>()
                val safeList = resultList

                if (!safeList.isNullOrEmpty()) {
                    // 🔥 Mejora: Caching de métodos para evitar el loop pesado de reflexión por cada página
                    val firstNonNull = safeList.firstOrNull { it != null }
                    if (firstNonNull != null) {
                        val c = firstNonNull.javaClass
                        val getImgUrlMethod = try { c.getMethod("getImageUrl") } catch (e: Exception) { null }
                        val getUrlMethod = try { c.getMethod("getUrl") } catch (e: Exception) { null }

                        for ((index, item) in safeList.withIndex()) {
                            if (item == null) continue
                            try {
                                var imageUrl = getImgUrlMethod?.invoke(item) as? String
                                if (imageUrl.isNullOrEmpty()) {
                                    imageUrl = getUrlMethod?.invoke(item) as? String ?: ""
                                }
                                if (!imageUrl.isNullOrEmpty()) {
                                    finalPages.add(PageInfo(index = index, imageUrl = imageUrl))
                                }
                            } catch (e: Exception) { }
                        }
                    }
                }

                return@withContext finalPages

            } catch (e: Throwable) {
                return@withContext emptyList()
            }
        }
    }

    private fun getPropertyValue(propertyName: String): Any? {
        return try {
            val method = extensionInstance.javaClass.getMethod(
                "get${propertyName.replaceFirstChar { it.uppercase() }}"
            )
            method.invoke(extensionInstance)
        } catch (e: Exception) {
            null
        }
    }
}