package com.example.shioriapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.shioriapp.domain.model.MangaInfo
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryManagerTest {

    private val testDispatcher = StandardTestDispatcher()

    // Variables para nuestro "celular falso"
    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // 1. Configuramos el almacenamiento falso para que Android no explote
        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)

        // Le enseñamos al Context falso cómo responder cuando la app intente guardar algo
        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor

        // Valores iniciales vacíos para que la app arranque
        every { mockPrefs.getString("library_data", any()) } returns "[]"
        every { mockPrefs.getString("reading_progress", any()) } returns "{}"
        every { mockPrefs.getBoolean("sort_descending", any()) } returns true

        // 2. Limpiamos la memoria del LibraryManager antes de cada prueba
        resetLibraryManager()

        // 3. Inicializamos
        LibraryManager.init(mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Función auxiliar que usa reflexión para vaciar las listas privadas
    @Suppress("UNCHECKED_CAST")
    private fun resetLibraryManager() {
        val isInitField = LibraryManager::class.java.getDeclaredField("isInitialized")
        isInitField.isAccessible = true
        isInitField.set(LibraryManager, false)

        val libField = LibraryManager::class.java.getDeclaredField("_library")
        libField.isAccessible = true
        (libField.get(LibraryManager) as MutableStateFlow<List<MangaInfo>>).value = emptyList()

        val progField = LibraryManager::class.java.getDeclaredField("_progressMap")
        progField.isAccessible = true
        (progField.get(LibraryManager) as MutableStateFlow<Map<String, ReadingProgress>>).value = emptyMap()
    }

    // --- COMIENZAN LAS PRUEBAS ---

    @Test
    fun toggleManga_agregaMangaSiNoExiste() = runTest {
        val manga = MangaInfo("Berserk", "/manga/berserk", "Test", "2", "")

        // Acción
        LibraryManager.toggleManga(mockContext, manga)

        // Comprobación
        val lista = LibraryManager.library.value
        assertTrue("El manga no se agregó a la lista", lista.any { it.title == "Berserk" })
    }

    @Test
    fun toggleManga_eliminaMangaSiYaExiste() = runTest {
        val manga = MangaInfo("Naruto", "/manga/naruto", "Test", "2", "")

        // Acción: Primer clic (Lo agrega)
        LibraryManager.toggleManga(mockContext, manga)

        // Acción: Segundo clic (Debería borrarlo)
        LibraryManager.toggleManga(mockContext, manga)

        // Comprobación
        val lista = LibraryManager.library.value
        assertFalse("El manga no se eliminó correctamente", lista.any { it.title == "Naruto" })
    }

    @Test
    fun saveProgress_guardaProgresoDeLecturaCorrectamente() = runTest {
        val mangaUrl = "/manga/one-piece"
        val capUrl = "/capitulo-1000"

        // Acción
        LibraryManager.saveProgress(
            context = mockContext,
            mangaUrl = mangaUrl,
            chapterUrl = capUrl,
            page = 15,
            totalPages = 20
        )

        // Comprobación
        val mapa = LibraryManager.progressMap.value
        assertTrue("El mapa de progreso está vacío", mapa.containsKey(mangaUrl))
        assertEquals("No se guardó el capítulo correcto", capUrl, mapa[mangaUrl]?.lastChapterUrl)
        assertEquals("No se guardó la página correcta", 15, mapa[mangaUrl]?.lastPage)
    }

    @Test
    fun unmarkAllChapters_borraElProgresoDelManga() = runTest {
        val mangaUrl = "/manga/dragon-ball"

        // Primero le damos un poco de progreso
        LibraryManager.saveProgress(mockContext, mangaUrl, "/cap-1", 10)

        // Ahora ejecutamos la función que borra el historial
        LibraryManager.unmarkAllChapters(mockContext, mangaUrl)

        // Comprobación
        val mapa = LibraryManager.progressMap.value
        assertFalse("El progreso no se borró", mapa.containsKey(mangaUrl))
    }
}