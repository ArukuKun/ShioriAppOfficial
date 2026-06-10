package com.example.shioriapp.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shioriapp.R
import com.example.shioriapp.auth.DiscordAuthService
import com.example.shioriapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

import com.example.shioriapp.MainActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val idToken = account.idToken
            android.util.Log.d("GOOGLE_AUTH", "ID Token obtenido correctamente: ${idToken?.take(10)}...")
            
            if (idToken != null) {
                authViewModel.loginWithGoogle(idToken)
            } else {
                android.util.Log.e("GOOGLE_AUTH", "Error: El ID Token es nulo.")
                authViewModel.setErrorMessage("Error de Google: El ID Token es nulo. Verifica la configuración de Firebase.")
            }
        } catch (e: com.google.android.gms.common.api.ApiException) {
            android.util.Log.e("GOOGLE_AUTH", "Error API Google: ${e.statusCode}", e)
            authViewModel.setErrorMessage("Error Google (Código ${e.statusCode}). Revisa el SHA-1 en Firebase.")
        } catch (e: Exception) {
            android.util.Log.e("GOOGLE_AUTH", "Error en el resultado de Google SignIn", e)
            authViewModel.setErrorMessage("Error inesperado: ${e.localizedMessage}")
        }
    }

    // Observamos el código que llega desde MainActivity (Deep Link)
    val externalCode = MainActivity.authCode
    LaunchedEffect(externalCode) {
        if (!externalCode.isNullOrEmpty()) {
            val success = authViewModel.handleDiscordCallback(externalCode)
            if (success) {
                onLoginSuccess()
                MainActivity.authCode = null // Limpiar después de usar
            }
        }
    }

    val discordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ -> 
        // El resultado vendrá por Intent a MainActivity, no por aquí
    }

    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Authenticated) {
            onLoginSuccess()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("ShioriApp", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (!isLoginMode) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nombre de usuario") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (errorMessage != null) {
                Text(errorMessage!!, color = Color.Red, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(
                onClick = {
                    if (isLoginMode) {
                        authViewModel.loginWithEmail(email, password)
                    } else {
                        authViewModel.registerWithEmail(email, password, displayName)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isLoginMode) "Iniciar sesión" else "Registrarse")
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = { isLoginMode = !isLoginMode }) {
                Text(if (isLoginMode) "¿No tienes cuenta? Regístrate" else "¿Ya tienes cuenta? Inicia sesión")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(DiscordAuthService.AUTHORIZE_URL))
                    discordLauncher.launch(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(painter = painterResource(id = android.R.drawable.ic_menu_share), contentDescription = "Discord")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continuar con Discord")
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = {
                    android.util.Log.d("GOOGLE_AUTH", "Iniciando proceso de login...")
                    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                    
                    // IMPORTANTE: Cerramos sesión previa para asegurar que siempre pida seleccionar cuenta
                    googleSignInClient.signOut().addOnCompleteListener {
                        android.util.Log.d("GOOGLE_AUTH", "Sesión previa limpiada. Lanzando selector de cuentas.")
                        googleLauncher.launch(googleSignInClient.signInIntent)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_compass), 
                    contentDescription = "Google",
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continuar con Google")
            }
        }
    }
}

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}