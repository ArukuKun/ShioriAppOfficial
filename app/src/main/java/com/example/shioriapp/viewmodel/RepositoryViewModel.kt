package com.example.shioriapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shioriapp.data.network.RetrofitClient
import com.example.shioriapp.data.repository.AppDatabase
import com.example.shioriapp.data.repository.RepositoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class RepositoryViewModel(application: Application) : AndroidViewModel(application) {

    // Conectamos con nuestra base de datos
    private val dao = AppDatabase.getDatabase(application).repositoryDao()

    // Esta es la lista que tu pantalla va a observar
    val repositories: StateFlow<List<RepositoryEntity>> = dao.getAllRepositories()
        .map { list ->
            if (list.isEmpty()) {
                val defaultRepo = RepositoryEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Keiyoushin Oficial",
                    url = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json",
                    isDefault = true
                )
                insertRepository(defaultRepo)
                listOf(defaultRepo)
            } else {
                list
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertRepository(repo: RepositoryEntity) {
        viewModelScope.launch {
            dao.insertRepository(repo)
        }
    }

    fun deleteRepository(repo: RepositoryEntity) {
        viewModelScope.launch {
            dao.deleteRepository(repo)
        }
    }

    fun testConnection(url: String) {
        viewModelScope.launch {
            try {
                Log.d("SHIORI_TEST", "viajando a internet... conectando a: $url")

                // ¡Aquí ocurre la magia!
                val results = RetrofitClient.api.getExtensions(url)

                Log.d("SHIORI_TEST", "¡BINGO! 🎉 Encontré ${results.size} extensiones.")
                if (results.isNotEmpty()) {
                    Log.d("SHIORI_TEST", "El primer anime de la lista es: ${results[0].name} (Idioma: ${results[0].lang})")
                }
            } catch (e: Exception) {
                Log.e("SHIORI_TEST", "Ups, algo falló en la descarga: ${e.message}")
            }
        }
    }
}

