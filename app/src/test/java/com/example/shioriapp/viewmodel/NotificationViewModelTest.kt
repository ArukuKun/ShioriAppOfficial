package com.example.shioriapp.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.ui.graphics.Color
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.navigation.NotificationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {

    private lateinit var viewModel: NotificationViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = NotificationViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addNotification_insertaCorrectamente() {
        val notif = NotificationItem(
            "Solo Leveling",
            "Capítulo 201",
            Icons.Default.Book,
            Color.Companion.Blue,
            "10:00"
        )

        viewModel.addNotification(notif)

        val lista = viewModel.notifications.value
        Assert.assertEquals(1, lista.size)
        Assert.assertEquals("Solo Leveling", lista[0].title)
    }

    @Test
    fun addNotification_previeneDuplicadosIdenticos() {
        val notif1 =
            NotificationItem(
                "One Piece",
                "Capítulo 1122",
                Icons.Default.Book,
                Color.Companion.Blue,
                "10:00"
            )
        val notif2 = NotificationItem(
            "One Piece",
            "Capítulo 1122",
            Icons.Default.Book,
            Color.Companion.Blue,
            "11:00"
        ) // Misma info, distinta hora

        viewModel.addNotification(notif1)
        viewModel.addNotification(notif2) // Intento de duplicado

        // La lista debe ignorar el segundo porque el título y descripción son idénticos
        val lista = viewModel.notifications.value
        Assert.assertEquals(1, lista.size)
    }

    @Test
    fun clearAll_borraTodasLasNotificaciones() {
        val notif =
            NotificationItem("Manga", "Desc", Icons.Default.Book, Color.Companion.Red, "12:00")
        viewModel.addNotification(notif)

        viewModel.clearAll()

        Assert.assertTrue(viewModel.notifications.value.isEmpty())
    }

    @Test
    fun checkLibraryUpdates_conListaVacia_noAgregaNotificaciones() {
        val listaBibliotecaVacia = emptyList<MangaInfo>()

        viewModel.checkLibraryUpdates(listaBibliotecaVacia)

        Assert.assertTrue(viewModel.notifications.value.isEmpty())
    }

    @Test
    fun checkLibraryUpdates_conMangasEnBiblioteca_generaNotificacionesCorrectas() {
        val mangaPrueba = MangaInfo(
            title = "Solo Leveling",
            url = "/manga/solo-leveling",
            sourceName = "FuentePrueba",
            status = 1,
            coverUrl = "https://url-falsa.com/imagen.jpg"
        )
        val listaBiblioteca = listOf(mangaPrueba)

        viewModel.checkLibraryUpdates(listaBiblioteca)

        val listaNotif = viewModel.notifications.value
        Assert.assertEquals(1, listaNotif.size)
        Assert.assertEquals("Solo Leveling", listaNotif[0].title)
        Assert.assertTrue(listaNotif[0].description.contains("Capítulo 201"))
        Assert.assertEquals("https://url-falsa.com/imagen.jpg", listaNotif[0].imageUrl)
    }
}