package com.example.shioriapp.viewmodel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shioriapp.data.repository.KeiyoushiRepository
import com.example.shioriapp.domain.model.ExtensionInfo
import com.example.shioriapp.extension.installer.ApkDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// NUEVO: Dividimos DOWNLOADING para saber si es instalación nueva o actualización
enum class InstallState { IDLE, DOWNLOADING_NEW, DOWNLOADING_UPDATE, INSTALLED, UPDATABLE, ERROR }

class ExtensionViewModel : ViewModel() {

    private val repository = KeiyoushiRepository()

    private val _extensions = MutableStateFlow<List<ExtensionInfo>>(emptyList())
    val extensions = _extensions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _installStates = MutableStateFlow<Map<String, InstallState>>(emptyMap())
    val installStates = _installStates.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var packageReceiver: BroadcastReceiver? = null

    fun loadExtensions(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = repository.fetchExtensions()
                _extensions.value = list
                refreshStates(context, list)
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar extensiones: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshStates(context: Context, list: List<ExtensionInfo> = _extensions.value) {
        val pm = context.packageManager
        _installStates.value = list.associate { ext ->
            ext.pkg to resolveState(pm, ext)
        }
    }

    private fun resolveState(pm: PackageManager, ext: ExtensionInfo): InstallState {
        return try {
            val installedCode = pm.getPackageInfo(ext.pkg, 0).longVersionCode
            if (installedCode < ext.code) InstallState.UPDATABLE else InstallState.INSTALLED
        } catch (e: PackageManager.NameNotFoundException) {
            InstallState.IDLE
        }
    }

    fun installExtension(context: Context, extension: ExtensionInfo) {
        viewModelScope.launch {
            // NUEVO: Revisamos el estado actual para saber qué tipo de descarga es
            val currentState = _installStates.value[extension.pkg]
            val isUpdate = currentState == InstallState.UPDATABLE

            setExtensionState(extension.pkg, if (isUpdate) InstallState.DOWNLOADING_UPDATE else InstallState.DOWNLOADING_NEW)

            try {
                val url = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/apk/${extension.apk}"
                ApkDownloader(context).downloadAndInstall(url = url, fileName = extension.pkg)
            } catch (e: Exception) {
                _errorMessage.value = "Error instalando ${extension.name}: ${e.message}"
                // En caso de error, refrescamos el estado real para que no quede atascado
                refreshStates(context)
            }
        }
    }

    fun uninstallExtension(context: Context, extension: ExtensionInfo) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:${extension.pkg}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        // No cambiamos el estado manualmente aquí. Android mostrará el cuadro de diálogo,
        // y cuando el usuario confirme y se desinstale, el BroadcastReceiver actualizará la lista.
    }

    fun clearError() { _errorMessage.value = null }

    private fun setExtensionState(pkg: String, state: InstallState) {
        _installStates.value = _installStates.value.toMutableMap().also { it[pkg] = state }
    }

    fun registerPackageReceiver(context: Context) {
        if (packageReceiver != null) return

        packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                refreshStates(c)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        context.registerReceiver(packageReceiver, filter)
    }

    fun unregisterPackageReceiver(context: Context) {
        packageReceiver?.let {
            context.unregisterReceiver(it)
            packageReceiver = null
        }
    }
}