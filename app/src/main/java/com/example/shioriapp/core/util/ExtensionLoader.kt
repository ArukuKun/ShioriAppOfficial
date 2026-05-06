package com.example.shioriapp.core.util

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
        return base.copy(
            description = try { rc.getMethod("getDescription").invoke(result) as? String ?: base.description } catch (e: Exception) { base.description },
            author      = try { rc.getMethod("getAuthor").invoke(result) as? String ?: base.author } catch (e: Exception) { base.author },
            coverUrl    = try { rc.getMethod("getThumbnail_url").invoke(result) as? String ?: base.coverUrl } catch (e: Exception) { base.coverUrl },
            status      = try { rc.getMethod("getStatus").invoke(result) as? Int ?: base.status } catch (e: Exception) { base.status }
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
                    mangaList.add(
                        MangaInfo(
                            title      = sManga.title,
                            url        = sManga.url,
                            coverUrl   = sManga.thumbnail_url ?: "",
                            author     = sManga.author ?: "",
                            status     = sManga.status,
                            sourceName = this.name
                        )
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
                val sManga = buildSManga(manga)

                val fetchMethod = findAllMethodsByName(extensionInstance.javaClass, "fetchMangaDetails").firstOrNull()
                if (fetchMethod != null) {
                    android.util.Log.d("SHIORI_DETAIL", "✅ Usando fetchMangaDetails (Observable)")
                    val observable = fetchMethod.invoke(extensionInstance, sManga)
                    val result = observable?.let {
                        val blocking = it.javaClass.getMethod("toBlocking").invoke(it)
                        blocking.javaClass.getMethod("first").invoke(blocking)
                    }
                    if (result != null) return@withContext extractMangaInfo(result, manga)
                }

                val getMethod = findAllMethodsByName(extensionInstance.javaClass, "getMangaDetails")
                    .firstOrNull { it.parameterCount == 2 } // SManga + Continuation
                if (getMethod != null) {
                    android.util.Log.d("SHIORI_DETAIL", "✅ Usando getMangaDetails (suspend)")
                    val result = kotlinx.coroutines.suspendCancellableCoroutine<Any?> { continuation ->
                        try {
                            val res = getMethod.invoke(extensionInstance, sManga, continuation)
                            if (res !== kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED) {
                                continuation.resumeWith(Result.success(res))
                            }
                        } catch (e: Exception) {
                            continuation.resumeWith(Result.failure(e))
                        }
                    }
                    if (result != null) return@withContext extractMangaInfo(result, manga)
                }

                val requestMethods = findAllMethodsByName(extensionInstance.javaClass, "mangaDetailsRequest")
                val parseMethods = findAllMethodsByName(extensionInstance.javaClass, "mangaDetailsParse")

                if (requestMethods.isNotEmpty() && parseMethods.isNotEmpty()) {
                    android.util.Log.d("SHIORI_DETAIL", "✅ Usando mangaDetailsRequest + parse")

                    val clientMethod = findAllMethodsByName(extensionInstance.javaClass, "getClient").firstOrNull()
                    val client = clientMethod?.invoke(extensionInstance) as? okhttp3.OkHttpClient

                    if (client != null) {
                        val reqMethod = requestMethods.firstOrNull { it.parameterCount == 1 }
                        val request = reqMethod?.invoke(extensionInstance, sManga) as? okhttp3.Request

                        if (request != null) {
                            val response = client.newCall(request).execute()
                            android.util.Log.d("SHIORI_DETAIL", "HTTP Code: ${response.code}")

                            val bodyString = response.body?.string() ?: ""
                            var result: Any? = null
                            for (parseMethod in parseMethods) {
                                val paramName = parseMethod.parameterTypes.firstOrNull()?.name ?: ""
                                try {


                                    if (paramName.contains("Document")) {
                                        android.util.Log.d("SHIORI_DETAIL", "Parseando con Jsoup Document")
                                        val document = org.jsoup.Jsoup.parse(bodyString, request.url.toString())
                                        result = parseMethod.invoke(extensionInstance, document)
                                        if (result != null) break

                                    } else if (paramName.contains("Response")) {
                                        android.util.Log.d("SHIORI_DETAIL", "Parseando con Response (Reconstruida)")
                                        val newBody = okhttp3.ResponseBody.create(response.body?.contentType(), bodyString)
                                        val newResponse = response.newBuilder().body(newBody).build()
                                        result = parseMethod.invoke(extensionInstance, newResponse)
                                        if (result != null) break

                                    } else {
                                        result = parseMethod.invoke(extensionInstance, bodyString)
                                        if (result != null) break
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("SHIORI_DETAIL", "Fallo al intentar parseMethod con $paramName", e)
                                }
                            }

                            if (result != null) return@withContext extractMangaInfo(result, manga)
                        }
                    }
                }

                android.util.Log.e("SHIORI_DETAIL", "❌ Ningún método funcionó al final.")
                return@withContext manga

            } catch (e: Throwable) {
                android.util.Log.e("SHIORI_DETAIL", "💀 ERROR fetchMangaDetails: ${e.message}", e)
                return@withContext manga
            }
        }
    }

    override suspend fun fetchChapterList(manga: MangaInfo): List<ChapterInfo> {
        return try {
            val method = extensionInstance.javaClass.methods
                .firstOrNull { it.name == "fetchChapterList" && it.parameterCount == 1 }
                ?: return emptyList()

            val sManga = buildSManga(manga)
            val observable = method.invoke(extensionInstance, sManga)
            val result = observable?.let {
                val blocking = it.javaClass.getMethod("toBlocking").invoke(it)
                blocking.javaClass.getMethod("first").invoke(blocking)
            } as? List<*>

            android.util.Log.d("ShioriApp", "fetchChapterList size: ${result?.size}")

            result?.mapNotNull { item ->
                if (item == null) return@mapNotNull null
                try {
                    val c = item.javaClass
                    ChapterInfo(
                        name          = c.getMethod("getName").invoke(item) as? String ?: "",
                        url           = c.getMethod("getUrl").invoke(item) as? String ?: "",
                        chapterNumber = c.getMethod("getChapter_number").invoke(item) as? Float ?: -1f,
                        dateUpload    = c.getMethod("getDate_upload").invoke(item) as? Long ?: 0L,
                        scanlator     = try { c.getMethod("getScanlator").invoke(item) as? String } catch (e: Exception) { null }
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ShioriApp", "Error mapeando capítulo", e)
                    null
                }
            } ?: emptyList()
        } catch (e: Throwable) {
            android.util.Log.e("ShioriApp", "Error fetchChapterList", e)
            emptyList()
        }
    }

    override suspend fun fetchPageList(chapter: ChapterInfo): List<PageInfo> {
        return try {
            val method = extensionInstance.javaClass.methods
                .firstOrNull { it.name == "fetchPageList" && it.parameterCount == 1 }
                ?: return emptyList()

            val sChapterClass = extensionInstance.javaClass.classLoader!!
                .loadClass("eu.kanade.tachiyomi.source.model.SChapterImpl")
            val sChapter = sChapterClass.getDeclaredConstructor().newInstance()

            sChapter.javaClass.getMethod("setUrl", String::class.java).invoke(sChapter, chapter.url)
            sChapter.javaClass.getMethod("setName", String::class.java).invoke(sChapter, chapter.name)

            val observable = method.invoke(extensionInstance, sChapter)
            val result = observable?.let {
                val blocking = it.javaClass.getMethod("toBlocking").invoke(it)
                blocking.javaClass.getMethod("first").invoke(blocking)
            } as? List<*>

            android.util.Log.d("ShioriApp", "fetchPageList size: ${result?.size}")

            result?.mapNotNull { item ->
                if (item == null) return@mapNotNull null
                try {
                    val c = item.javaClass
                    PageInfo(
                        index    = c.getMethod("getIndex").invoke(item) as? Int ?: 0,
                        url      = c.getMethod("getUrl").invoke(item) as? String ?: "",
                        imageUrl = try { c.getMethod("getImageUrl").invoke(item) as? String } catch (e: Exception) { null }
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ShioriApp", "Error mapeando página", e)
                    null
                }
            } ?: emptyList()
        } catch (e: Throwable) {
            android.util.Log.e("ShioriApp", "Error fetchPageList", e)
            emptyList()
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