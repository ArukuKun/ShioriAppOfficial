package com.example.shioriapp.viewmodel

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val mockContext = mockk<Context>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 🔥 Adiós a las comillas invertidas, usamos nombres estándar
    @Test
    fun estadoInicial_deberiaSerUnauthenticatedOLoading() {
        val estadoActual = viewModel.authState.value

        val noEstaAutenticado = estadoActual is AuthViewModel.AuthState.Unauthenticated
                || estadoActual is AuthViewModel.AuthState.Loading

        assertTrue("El estado inicial no es correcto", noEstaAutenticado)
    }

    @Test
    fun alHacerLogout_elEstadoCambiaAUnauthenticated() = runTest {
        viewModel.logout()

        testDispatcher.scheduler.advanceUntilIdle()

        val estadoActual = viewModel.authState.value
        assertTrue(estadoActual is AuthViewModel.AuthState.Unauthenticated)
    }
}