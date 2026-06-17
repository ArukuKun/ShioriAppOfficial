package com.example.shioriapp.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("shiori_appearance", Context.MODE_PRIVATE)

    // Estados
    var selectedTheme by remember { mutableIntStateOf(prefs.getInt("theme_mode", 0)) }
    var amoledBlack by remember { mutableStateOf(prefs.getBoolean("amoled_black", false)) }

    var bgUri by remember { mutableStateOf(prefs.getString("bg_uri", "")?.let { if (it.isNotEmpty()) Uri.parse(it) else null }) }
    var blurLevel by remember { mutableFloatStateOf(prefs.getFloat("bg_blur", 0f)) }
    var opacityLevel by remember { mutableFloatStateOf(prefs.getFloat("bg_opacity", 1f)) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // Persistir permiso de acceso al archivo
            context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            bgUri = it
            prefs.edit().putString("bg_uri", it.toString()).apply()
            Toast.makeText(context, "Fondo actualizado", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apariencia", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // --- SECCIÓN TEMA ---
            Text("TEMA", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
            ThemeSelectionRow("Predeterminado del sistema", selectedTheme == 0) {
                selectedTheme = 0
                prefs.edit().putInt("theme_mode", 0).apply()
            }
            ThemeSelectionRow("Claro", selectedTheme == 1) {
                selectedTheme = 1
                prefs.edit().putInt("theme_mode", 1).apply()
            }
            ThemeSelectionRow("Oscuro", selectedTheme == 2) {
                selectedTheme = 2
                prefs.edit().putInt("theme_mode", 2).apply()
            }

            HorizontalDivider(modifier = Modifier.padding(16.dp))

            // --- SECCIÓN FONDO PERSONALIZADO ---
            Text("FONDO DE PANTALLA", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(16.dp))

            Button(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Image, null)
                Spacer(Modifier.width(8.dp))
                Text(if (bgUri == null) "Seleccionar Fondo" else "Cambiar Imagen")
            }

            if (bgUri != null) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Image(
                        painter = rememberAsyncImagePainter(bgUri),
                        contentDescription = "Fondo",
                        modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outline),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Difuminación: ${blurLevel.toInt()}px", fontSize = 14.sp)
                    Slider(value = blurLevel, onValueChange = { blurLevel = it }, valueRange = 0f..50f, onValueChangeFinished = { prefs.edit().putFloat("bg_blur", blurLevel).apply() })

                    Text("Opacidad: ${(opacityLevel * 100).toInt()}%", fontSize = 14.sp)
                    Slider(value = opacityLevel, onValueChange = { opacityLevel = it }, valueRange = 0f..1f, onValueChangeFinished = { prefs.edit().putFloat("bg_opacity", opacityLevel).apply() })
                }
            }

            HorizontalDivider(modifier = Modifier.padding(16.dp))

            // --- SECCIÓN AMOLED ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { amoledBlack = !amoledBlack; prefs.edit().putBoolean("amoled_black", amoledBlack).apply() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Negro puro (AMOLED)", fontSize = 16.sp)
                    Text("Ahorra batería en pantallas OLED.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = amoledBlack, onCheckedChange = { amoledBlack = it; prefs.edit().putBoolean("amoled_black", it).apply() })
            }
        }
    }
}

@Composable
fun ThemeSelectionRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 16.sp)
    }
}