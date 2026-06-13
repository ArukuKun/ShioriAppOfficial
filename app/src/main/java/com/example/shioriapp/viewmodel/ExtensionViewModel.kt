package com.example.shioriapp.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shioriapp.data.network.RetrofitClient
import com.example.shioriapp.data.repository.AppDatabase
import com.example.shioriapp.domain.model.ExtensionInfo
import com.example.shioriapp.extension.installer.ApkDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class InstallState { IDLE, DOWNLOADING_NEW, DOWNLOADING_UPDATE, INSTALLED, UPDATABLE, ERROR }

class ExtensionViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).repositoryDao()

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
            // 🔥 CAMBIO CLAVE: Usamos collect para escuchar la base de datos en TIEMPO REAL
            dao.getAllRepositories().collect { savedRepos ->
                _isLoading.value = true
                Log.d("SHIORI_DEBUG", "Repositorios detectados en la BD: ${savedRepos.size}")

                if (savedRepos.isEmpty()) {
                    Log.w("SHIORI_DEBUG", "No hay repositorios. Ve a la pantalla de repositorios y agrega uno.")
                    _extensions.value = emptyList() // Limpia la lista
                    _isLoading.value = false
                    return@collect // Corta esta ejecución, pero sigue escuchando
                }

                val allExtensions = mutableListOf<ExtensionInfo>()

                // 2. Busca las extensiones en los links que encontraste
                for (repo in savedRepos) {
                    try {
                        Log.d("SHIORI_DEBUG", "Descargando desde: ${repo.url}")
                        val listFromInternet = RetrofitClient.api.getExtensions(repo.url)
                        val baseUrl = repo.url.substringBeforeLast("index.min.json")
                        listFromInternet.forEach { it.repoBaseUrl = baseUrl }
                        allExtensions.addAll(listFromInternet)
                    } catch (e: Exception) {
                        Log.e("SHIORI_DEBUG", "Error con el repo ${repo.name}: ${e.message}")
                    }
                }

                // 3. Filtra y muestra
                val uniqueExtensions = allExtensions.distinctBy { it.pkg }
                Log.d("SHIORI_DEBUG", "¡Éxito! Mostrando ${uniqueExtensions.size} extensiones.")
                _extensions.value = uniqueExtensions
                refreshStates(context, uniqueExtensions)

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
            val packageInfo = pm.getPackageInfo(ext.pkg, 0)

            val installedCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            if (installedCode < ext.code) InstallState.UPDATABLE else InstallState.INSTALLED
        } catch (e: PackageManager.NameNotFoundException) {
            InstallState.IDLE
        }
    }

    fun installExtension(context: Context, extension: ExtensionInfo) {
        viewModelScope.launch {
            val currentState = _installStates.value[extension.pkg]
            val isUpdate = currentState == InstallState.UPDATABLE

            setExtensionState(extension.pkg, if (isUpdate) InstallState.DOWNLOADING_UPDATE else InstallState.DOWNLOADING_NEW)

            try {
                val url = "${extension.repoBaseUrl}apk/${extension.apk}"
                ApkDownloader(context).downloadAndInstall(url = url, fileName = extension.pkg)
            } catch (e: Exception) {
                _errorMessage.value = "Error instalando ${extension.name}: ${e.message}"
                refreshStates(context)
            }
        } // ¡Y FALTABA ESTE CORCHETE!
    }

    fun uninstallExtension(context: Context, extension: ExtensionInfo) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${extension.pkg}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _errorMessage.value = "Fallo al desinstalar. Permisos insuficientes."
        }
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