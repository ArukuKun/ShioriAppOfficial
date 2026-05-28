package com.example.shioriapp.screens

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettingsScreen(
    currentPath: String,
    onBack: () -> Unit,
    onFolderChange: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("shiori_storage", Context.MODE_PRIVATE)

    // ── ESTADOS DE PERSISTENCIA ──
    var autoDeleteEnabled by remember { mutableStateOf(prefs.getBoolean("auto_delete", false)) }
    var wifiOnlyEnabled by remember { mutableStateOf(prefs.getBoolean("wifi_only", true)) }

    // ── ESTADOS DE ALMACENAMIENTO ──
    var usedBytes by remember { mutableStateOf(0L) }
    var totalBytes by remember { mutableStateOf(1L) } // 1L para evitar división por cero
    var cacheBytes by remember { mutableStateOf(0L) }
    var isCalculating by remember { mutableStateOf(true) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { onFolderChange(it) }
    }

    // ── LÓGICA: CÁLCULO DE ESPACIO EN SEGUNDO PLANO ──
    fun recalculateSpace() {
        scope.launch(Dispatchers.IO) {
            isCalculating = true
            try {
                // 1. Calcular espacio total del dispositivo
                val statFs = StatFs(Environment.getDataDirectory().path)
                val totalDeviceSpace = statFs.blockCountLong * statFs.blockSizeLong

                // 2. Calcular caché de la app
                val currentCacheSize = getFolderSize(context.cacheDir)

                // 3. Calcular peso de la carpeta de descargas (si existe)
                var downloadsSize = 0L
                if (currentPath.startsWith("content://")) {
                    val uri = Uri.parse(currentPath)
                    val docFile = DocumentFile.fromTreeUri(context, uri)
                    downloadsSize = getDocumentTreeSize(docFile)
                }

                withContext(Dispatchers.Main) {
                    totalBytes = totalDeviceSpace
                    usedBytes = currentCacheSize + downloadsSize
                    cacheBytes = currentCacheSize
                    isCalculating = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isCalculating = false }
            }
        }
    }

    // Ejecutar al abrir la pantalla
    LaunchedEffect(currentPath) {
        recalculateSpace()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Almacenamiento y Descargas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // ── 1. GRÁFICA DE ESPACIO ──
            Spacer(modifier = Modifier.height(16.dp))
            Text("Espacio utilizado por la App", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            if (isCalculating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp))
                Text("Calculando espacio...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val progress = (usedBytes.toDouble() / totalBytes.toDouble()).coerceIn(0.0, 1.0).toFloat()
                Text("Usado: ${formatBytes(usedBytes)} de ${formatBytes(totalBytes)}", fontSize = 14.sp)

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 2. RUTA DE DESCARGAS ──
            Text("Ubicación", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedCard(
                onClick = { launcher.launch(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Carpeta de descargas", fontWeight = FontWeight.Bold)
                        Text(currentPath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 3. GESTIÓN Y LIMPIEZA ──
            Text("Gestión", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            // Lógica: Limpiar Caché
            ListItem(
                headlineContent = { Text("Limpiar Caché de Imágenes") },
                supportingContent = {
                    val cacheText = if (isCalculating) "Calculando..." else "Libera espacio temporal (${formatBytes(cacheBytes)})"
                    Text(cacheText)
                },
                leadingContent = { Icon(Icons.Default.CleaningServices, contentDescription = null) },
                modifier = Modifier.clickable {
                    scope.launch(Dispatchers.IO) {
                        context.cacheDir.deleteRecursively() // Borra la caché real
                        context.cacheDir.mkdirs() // Recrea la carpeta base
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Caché limpiada correctamente", Toast.LENGTH_SHORT).show()
                            recalculateSpace() // Actualiza la UI
                        }
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Lógica: Limpiar Residuales (Carpetas vacías)
            ListItem(
                headlineContent = { Text("Limpiar carpetas vacías") },
                supportingContent = { Text("Elimina carpetas de descargas sin archivos dentro") },
                leadingContent = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                modifier = Modifier.clickable {
                    if (currentPath.startsWith("content://")) {
                        scope.launch(Dispatchers.IO) {
                            var deletedCount = 0
                            val docFile = DocumentFile.fromTreeUri(context, Uri.parse(currentPath))
                            docFile?.listFiles()?.forEach { file ->
                                if (file.isDirectory && file.listFiles().isEmpty()) {
                                    file.delete()
                                    deletedCount++
                                }
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Se eliminaron $deletedCount carpetas residuales", Toast.LENGTH_SHORT).show()
                                recalculateSpace()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Selecciona una carpeta válida primero", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── 4. REGLAS AUTOMÁTICAS ──
            Text("Reglas de Descarga", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("Auto-eliminar capítulos leídos") },
                supportingContent = { Text("Borra los archivos de tu dispositivo tras leerlos") },
                trailingContent = {
                    Switch(
                        checked = autoDeleteEnabled,
                        onCheckedChange = {
                            autoDeleteEnabled = it
                            prefs.edit().putBoolean("auto_delete", it).apply()
                        }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("Descargar solo por Wi-Fi") },
                supportingContent = { Text("Pausa las descargas si usas datos móviles") },
                trailingContent = {
                    Switch(
                        checked = wifiOnlyEnabled,
                        onCheckedChange = {
                            wifiOnlyEnabled = it
                            prefs.edit().putBoolean("wifi_only", it).apply()
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ── UTILIDADES DE SISTEMA DE ARCHIVOS ──

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun getFolderSize(file: File): Long {
    var size: Long = 0
    if (file.exists()) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                size += getFolderSize(child)
            }
        } else {
            size = file.length()
        }
    }
    return size
}

fun getDocumentTreeSize(docFile: DocumentFile?): Long {
    var size: Long = 0
    if (docFile != null && docFile.exists()) {
        if (docFile.isDirectory) {
            docFile.listFiles().forEach { child ->
                size += getDocumentTreeSize(child)
            }
        } else {
            size = docFile.length()
        }
    }
    return size
}