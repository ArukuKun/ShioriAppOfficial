package com.example.shioriapp.core.util

import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.shioriapp.domain.model.ChapterInfo
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.domain.model.PageInfo
import dalvik.system.PathClassLoader
import com.example.shioriapp.domain.source.Source
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton

object ExtensionLoader {

    private var isInjektInitialized = false

    // 🔥 LA BÓVEDA: Guardamos las extensiones por su nombre (ej: "Manhwa Scan")
    private val activeSources = mutableMapOf<String, Source>()

    // Función súper rápida para obtener la extensión sin usar el PackageManager
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

            for (appInfo in apps) {
                val isExtension = appInfo.packageName.contains("tachiyomi.extension") ||
                        appInfo.metaData?.containsKey("tachiyomi.extension.class") == true

                if (isExtension) {
                    val loadedSources = loadExtensionList(context, appInfo.packageName)
                    sources.addAll(loadedSources)

                    // 🔥 GUARDAMOS LA EXTENSIÓN EN LA BÓVEDA AL INSTANTE
                    loadedSources.forEach { source ->
                        activeSources[source.name] = source
                    }
                }
            }
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
                        addSingleton(eu.kanade.tachiyomi.network.NetworkHelper())
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

            var sourceClassName = appInfo.metaData?.getString("tachiyomi.extension.class")
            if (sourceClassName == null) return emptyList()

            if (sourceClassName.startsWith(".")) {
                sourceClassName = pkgName + sourceClassName
            }

            val classLoader = PathClassLoader(
                appInfo.sourceDir,
                appInfo.nativeLibraryDir,
                context.classLoader
            )

            val sourceClass = Class.forName(sourceClassName, false, classLoader)
            val sourceInstance = sourceClass.newInstance()

            if (sourceInstance is eu.kanade.tachiyomi.source.SourceFactory) {
                val sources = sourceInstance.createSources()
                for (source in sources) {
                    val adapter = SourceAdapter(source, pkgName, packageManager, appInfo)
                    generatedSources.add(adapter)
                    android.util.Log.d("ShioriApp", "🌍 Fábrica generó: ${adapter.name} (${source.lang})")
                }
            } else if (sourceInstance is eu.kanade.tachiyomi.source.Source) {
                val adapter = SourceAdapter(sourceInstance, pkgName, packageManager, appInfo)
                generatedSources.add(adapter)
                android.util.Log.d("ShioriApp", "📄 Extensión cargada: ${adapter.name} (${sourceInstance.lang})")
            } else {
                android.util.Log.w("ShioriApp", "La clase no es ni Source ni SourceFactory")
            }

            return generatedSources

        } catch (e: Throwable) {
            android.util.Log.e("ShioriApp", "💀 ERROR AL CARGAR EXTENSIÓN:", e)
            return emptyList()
        }
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
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
        get() = packageManager.getApplicationLabel(appInfo).toString().removePrefix("Tachiyomi: ").trim()

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
        val newTitle  = try { rc.getMethod("getTitle").invoke(result) as? String } catch (e: Exception) { null }  // 👈
        val newDesc   = try { rc.getMethod("getDescription").invoke(result) as? String } catch (e: Exception) { null }
        val newAuthor = try { rc.getMethod("getAuthor").invoke(result) as? String } catch (e: Exception) { null }
        val newCover  = try { rc.getMethod("getThumbnail_url").invoke(result) as? String } catch (e: Exception) { null }
        val newStatus = try { rc.getMethod("getStatus").invoke(result) as? Int } catch (e: Exception) { null }

        return base.copy(
            title       = if (!newTitle.isNullOrBlank() && newTitle != "Manga") newTitle else base.title,  // 👈
            description = if (!newDesc.isNullOrBlank()) newDesc else base.description,
            author      = if (!newAuthor.isNullOrBlank()) newAuthor else base.author,
            coverUrl    = if (!newCover.isNullOrBlank()) newCover else base.coverUrl,
            status      = newStatus ?: base.status
        )
    }

    override suspend fun fetchSearchManga(query: String, page: Int): List<MangaInfo> {
        val mangaList = mutableListOf<MangaInfo>()
        try {
            val method = extensionInstance.javaClass.getMethod(
                "fetchSearchManga",
                Int::class.java,
                String::class.java,
                eu.kanade.tachiyomi.source.model.FilterList::class.java
            )
            val emptyFilters = eu.kanade.tachiyomi.source.model.FilterList(emptyList())
            val observable = method.invoke(extensionInstance, page, query, emptyFilters)

            if (observable != null) {
                val blocking = observable.javaClass.getMethod("toBlocking").invoke(observable)
                val result = blocking.javaClass.getMethod("first").invoke(blocking)
                        as? eu.kanade.tachiyomi.source.model.MangasPage

                result?.mangas?.forEach { sManga ->
                    android.util.Log.d(TAG, "📦 Recibido de extensión -> Título: ${sManga.title} | Géneros: ${sManga.genre}")
                    mangaList.add(
                        MangaInfo(
                            title      = sManga.title,
                            url        = sManga.url,
                            coverUrl   = sManga.thumbnail_url ?: "",
                            author     = sManga.author ?: "",
                            status     = sManga.status,
                            sourceName = this.name,
                            genres     = (sManga.genre ?: "").split(",").map { it.trim() }.filter { it.isNotBlank() }                        )
                    )
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("ShioriApp", "Error de búsqueda en ${this.name}", e)
        }
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
                                if (res !== kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED) {
                                    continuation.resumeWith(Result.success(res))
                                }
                            } catch (e: Exception) {
                                continuation.resumeWith(Result.failure(e))
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
                                // 🔥 ¡El gran truco! Construimos la petición a mano porque tu app no tiene el método base
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
                                val response = client.newCall(request).execute()
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
                    }
                }

                // Devolvemos la info limpia
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
            android.util.Log.e(TAG, "==================================================")
            android.util.Log.e(TAG, "🔍 INICIANDO EXTRACCIÓN DE CAPÍTULOS PARA: ${manga.title}")

            try {
                val sMangaClass = extensionInstance.javaClass.classLoader!!.loadClass("eu.kanade.tachiyomi.source.model.SMangaImpl")
                val sManga = sMangaClass.getDeclaredConstructor().newInstance()
                sManga.javaClass.getMethod("setUrl", String::class.java).invoke(sManga, manga.url)
                sManga.javaClass.getMethod("setTitle", String::class.java).invoke(sManga, manga.title)

                val allMethods = extensionInstance.javaClass.methods
                var resultList: List<*>? = null

                // DIAGNÓSTICO
                val hasGetChapterList = allMethods.any { it.name == "getChapterList" && it.parameterCount == 2 }
                val hasFetchChapterList = allMethods.any { it.name == "fetchChapterList" && it.parameterCount == 1 }
                android.util.Log.e(TAG, "🔎 Métodos disponibles en la extensión:")
                android.util.Log.e(TAG, "   - getChapterList (API Moderna Suspend): $hasGetChapterList")
                android.util.Log.e(TAG, "   - fetchChapterList (API Vieja RxJava): $hasFetchChapterList")

                // ── Intento A: API Moderna (Suspend) ──
                if (hasGetChapterList) {
                    android.util.Log.e(TAG, "▶️ Ejecutando Intento A (Suspend)...")
                    val getMethod = allMethods.first { it.name == "getChapterList" && it.parameterCount == 2 }
                    try {
                        val res = kotlinx.coroutines.suspendCancellableCoroutine<Any?> { continuation ->
                            try {
                                val invokeRes = getMethod.invoke(extensionInstance, sManga, continuation)
                                if (invokeRes !== kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED) {
                                    continuation.resumeWith(Result.success(invokeRes))
                                }
                            } catch (e: Exception) {
                                continuation.resumeWith(Result.failure(e))
                            }
                        }
                        resultList = res as? List<*>
                        android.util.Log.e(TAG, "   ✅ Intento A devolvió una lista de tamaño: ${resultList?.size ?: "nulo"}")
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "   ❌ Intento A falló: ${e.message}")
                    }
                }

                // ── Intento B: API Antigua (RxJava) ──
                if (resultList.isNullOrEmpty() && hasFetchChapterList) {
                    android.util.Log.e(TAG, "▶️ Ejecutando Intento B (RxJava)...")
                    val fetchMethod = allMethods.first { it.name == "fetchChapterList" && it.parameterCount == 1 }
                    try {
                        val observable = fetchMethod.invoke(extensionInstance, sManga)
                        if (observable != null) {
                            val blocking = observable.javaClass.getMethod("toBlocking").invoke(observable)
                            resultList = blocking.javaClass.getMethod("first").invoke(blocking) as? List<*>
                            android.util.Log.e(TAG, "   ✅ Intento B devolvió una lista de tamaño: ${resultList?.size ?: "nulo"}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "   ❌ Intento B falló: ${e.message}")
                    }
                }

                // ── Intento C: MODO SUPERVIVENCIA HTTP ──
                if (resultList.isNullOrEmpty()) {
                    android.util.Log.e(TAG, "⚠️ A y B no trajeron capítulos. ▶️ Ejecutando Intento C (HTTP Manual)...")
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
                                android.util.Log.e(TAG, "   🌐 Petición GET a: ${request.url}")
                                val response = client.newCall(request).execute()
                                val bodyString = response.body?.string() ?: ""
                                android.util.Log.e(TAG, "   📥 HTTP ${response.code} | Longitud HTML: ${bodyString.length}")

                                for (parseMethod in parseMethods) {
                                    try {
                                        val paramType = parseMethod.parameterTypes.firstOrNull()?.name ?: ""
                                        android.util.Log.e(TAG, "   🛠 Probando parseMethod con: $paramType")

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
                                            android.util.Log.e(TAG, "   ✅ Parse exitoso.")
                                            break
                                        }
                                        // 🔥 AQUÍ ESTÁ LA MAGIA PARA VER EL ERROR REAL:
                                    } catch (e: java.lang.reflect.InvocationTargetException) {
                                        android.util.Log.e(TAG, "   ❌ parseMethod falló internamente: ${e.targetException}")
                                        e.targetException?.printStackTrace()
                                    } catch (e: Exception) {
                                        android.util.Log.e(TAG, "   ❌ parseMethod falló por otro motivo: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                }

                // ── MAPEO DE DATOS A TU INTERFAZ ──
                val finalChapterList = mutableListOf<ChapterInfo>()
                if (resultList != null) {
                    for (item in resultList) {
                        if (item == null) continue
                        try {
                            val c = item.javaClass
                            finalChapterList.add(
                                ChapterInfo(
                                    name = c.getMethod("getName").invoke(item) as? String ?: "Capítulo sin nombre",
                                    url = c.getMethod("getUrl").invoke(item) as? String ?: "",
                                    chapterNumber = try { c.getMethod("getChapter_number").invoke(item) as? Float ?: -1f } catch (e: Exception) { -1f },
                                    dateUpload = try { c.getMethod("getDate_upload").invoke(item) as? Long ?: 0L } catch (e: Exception) { 0L },
                                    scanlator = try { c.getMethod("getScanlator").invoke(item) as? String } catch (e: Exception) { null }
                                )
                            )
                        } catch (e: Exception) {
                            android.util.Log.e(TAG, "   ❌ Error mapeando capítulo individual: ${e.message}")
                        }
                    }
                }

                android.util.Log.e(TAG, "🏁 RESULTADO FINAL: ${finalChapterList.size} capítulos listos para la UI.")
                return@withContext finalChapterList

            } catch (e: Throwable) {
                android.util.Log.e(TAG, "💀 ERROR FATAL GIGANTE:", e)
                return@withContext emptyList()
            }
        }
    }

    override suspend fun fetchPageList(chapter: ChapterInfo): List<PageInfo> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val TAG = "SHIORI_PAGES"
            android.util.Log.e(TAG, "==================================================")
            android.util.Log.e(TAG, "🔍 INICIANDO EXTRACCIÓN DE PÁGINAS PARA: ${chapter.name}")

            try {
                // 1. Creamos el SChapter
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
                    android.util.Log.e(TAG, "▶️ Ejecutando Intento A (Suspend)...")
                    val getMethod = allMethods.first { it.name == "getPageList" && it.parameterCount == 2 }
                    try {
                        val res = kotlinx.coroutines.suspendCancellableCoroutine<Any?> { continuation ->
                            try {
                                val invokeRes = getMethod.invoke(extensionInstance, sChapter, continuation)
                                if (invokeRes !== kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED) {
                                    continuation.resumeWith(Result.success(invokeRes))
                                }
                            } catch (e: Exception) {
                                continuation.resumeWith(Result.failure(e))
                            }
                        }
                        resultList = res as? List<*>
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "   ❌ Intento A falló: ${e.message}")
                    }
                }

                // ── Intento B: API Antigua (RxJava) ──
                if (resultList.isNullOrEmpty() && hasFetchPageList) {
                    android.util.Log.e(TAG, "▶️ Ejecutando Intento B (RxJava)...")
                    val fetchMethod = allMethods.first { it.name == "fetchPageList" && it.parameterCount == 1 }
                    try {
                        val observable = fetchMethod.invoke(extensionInstance, sChapter)
                        if (observable != null) {
                            val blocking = observable.javaClass.getMethod("toBlocking").invoke(observable)
                            resultList = blocking.javaClass.getMethod("first").invoke(blocking) as? List<*>
                        }
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "   ❌ Intento B falló: ${e.message}")
                    }
                }

                // 🔥 ── Intento C: MODO SUPERVIVENCIA HTTP (El que faltaba) ──
                if (resultList.isNullOrEmpty()) {
                    android.util.Log.e(TAG, "⚠️ A y B fallaron. ▶️ Ejecutando Intento C (HTTP Manual)...")
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
                                android.util.Log.e(TAG, "   🌐 Petición GET a: ${request.url}")
                                val response = client.newCall(request).execute()
                                val bodyString = response.body?.string() ?: ""
                                android.util.Log.e(TAG, "   📥 HTTP ${response.code} | Longitud HTML: ${bodyString.length}")

                                for (parseMethod in parseMethods) {
                                    try {
                                        var tempResult: Any? = null
                                        val paramType = parseMethod.parameterTypes.firstOrNull()?.name ?: ""
                                        android.util.Log.e(TAG, "   🛠 Probando pageListParse con: $paramType")

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
                                            android.util.Log.e(TAG, "   ✅ Parse C exitoso.")
                                            break
                                        }
                                    } catch (e: java.lang.reflect.InvocationTargetException) {
                                        android.util.Log.e(TAG, "   ❌ parseMethod C falló internamente: ${e.targetException}")
                                        e.targetException?.printStackTrace()
                                    } catch (e: Exception) {
                                        android.util.Log.e(TAG, "   ❌ parseMethod C falló por otro motivo: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                }

                // ── TRADUCCIÓN A NUESTRO FORMATO ──
                val finalPages = mutableListOf<PageInfo>()
                if (resultList != null) {
                    for ((index, item) in resultList.withIndex()) {
                        if (item == null) continue
                        try {
                            val c = item.javaClass
                            var imageUrl = c.getMethod("getImageUrl").invoke(item) as? String
                            if (imageUrl.isNullOrEmpty()) {
                                imageUrl = c.getMethod("getUrl").invoke(item) as? String ?: ""
                            }
                            if (!imageUrl.isNullOrEmpty()) {
                                finalPages.add(PageInfo(index = index, imageUrl = imageUrl))
                            }
                        } catch (e: Exception) {
                            android.util.Log.e(TAG, "   ❌ Error mapeando página: ${e.message}")
                        }
                    }
                }

                android.util.Log.e(TAG, "🏁 RESULTADO FINAL: ${finalPages.size} páginas extraídas y listas.")
                return@withContext finalPages

            } catch (e: Throwable) {
                android.util.Log.e(TAG, "💀 ERROR FATAL AL EXTRAER PÁGINAS:", e)
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