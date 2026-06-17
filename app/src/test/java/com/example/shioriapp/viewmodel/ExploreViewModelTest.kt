package com.example.shioriapp.viewmodel

import com.example.shioriapp.core.util.ExtensionLoader
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ExploreViewModelTest {

    private lateinit var viewModel: ExploreViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockkObject(ExtensionLoader)
        every { ExtensionLoader.getAvailableSources() } returns listOf("FuenteFalsa")
        every { ExtensionLoader.getSource(any()) } returns null

        viewModel = ExploreViewModel()
    }

    @After
    fun tearDown() {
        Thread.sleep(200)
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun toggleSourcesView_cambiaElEstadoCorrectamente() {
        assertFalse(viewModel.state.value.isShowingSources)
        viewModel.toggleSourcesView(true)
        assertTrue(viewModel.state.value.isShowingSources)
    }

    @Test
    fun setCategory_actualizaCategoriaYFiltra() = runTest {
        viewModel.setCategory("Acción")

        Thread.sleep(200) // Pausa real para que Dispatchers.IO procese

        val state = viewModel.state.value
        assertEquals("Acción", state.selectedCategory)
        assertEquals(1, state.page)
    }

    @Test
    fun setCategory_siEsTodo_devuelveListaCompleta() = runTest {
        viewModel.setCategory("Acción")
        Thread.sleep(200)

        viewModel.setCategory("Todo")
        Thread.sleep(200)

        assertEquals("Todo", viewModel.state.value.selectedCategory)
    }

    @Test
    fun openExtensionCatalog_preparaElEstadoParaUnaNuevaFuente() = runTest {
        val nombreFuente = "MangaDex"
        viewModel.openExtensionCatalog(nombreFuente)

        Thread.sleep(200) // Pausa real

        val state = viewModel.state.value
        assertEquals(nombreFuente, state.selectedExtension)
        assertEquals(1, state.extensionPage)
        assertEquals(0, state.extensionTab)
        assertFalse(state.isExtensionLoading)
        assertTrue(state.extensionMangas.isEmpty())
    }

    @Test
    fun setExtensionTab_cambiaLaPestanaYReiniciaPaginacion() = runTest {
        viewModel.openExtensionCatalog("FuenteFalsa")
        Thread.sleep(200)

        viewModel.setExtensionTab(1)
        Thread.sleep(200)

        val state = viewModel.state.value
        assertEquals(1, state.extensionTab)
        assertEquals(1, state.extensionPage)
    }

    @Test
    fun closeExtensionCatalog_limpiaLaFuenteSeleccionada() = runTest {
        viewModel.openExtensionCatalog("FuenteFalsa")
        Thread.sleep(200)

        viewModel.closeExtensionCatalog()

        val state = viewModel.state.value
        assertEquals(null, state.selectedExtension)
        assertTrue(state.extensionMangas.isEmpty())
    }
}