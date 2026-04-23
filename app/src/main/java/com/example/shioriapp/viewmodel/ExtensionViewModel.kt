package com.example.shioriapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shioriapp.data.repository.KeiyoushiRepository
import com.example.shioriapp.domain.model.ExtensionInfo
import com.example.shioriapp.extension.installer.ApkDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExtensionViewModel : ViewModel() {
    private val repository = KeiyoushiRepository()

    private val _extensions = MutableStateFlow<List<ExtensionInfo>>(emptyList())
    val extensions = _extensions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadExtensions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // CORRECCIÓN 1: Usamos el método real de tu repositorio
                _extensions.value = repository.fetchExtensions()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun installExtension(context: Context, extension: ExtensionInfo) {
        viewModelScope.launch {
            val downloader = ApkDownloader(context)

            val apkUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/apk/${extension.apk}"

            downloader.downloadAndInstall(url = apkUrl, fileName = extension.pkg)
        }
    }
}