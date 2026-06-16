package com.example.shioriapp.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shioriapp.auth.DiscordAuthService
import com.example.shioriapp.auth.FirebaseAuthManager
import com.example.shioriapp.auth.UserManager
import com.example.shioriapp.domain.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(private val context: Context) : ViewModel() {

    // 1. ESCUDO: Si Firebase no está inicializado, lo encendemos a la fuerza de manera segura.
    init {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                android.util.Log.d("AuthViewModel", "Firebase initialized in ViewModel fallback.")
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Failed to initialize Firebase in ViewModel", e)
        }
    }

    // 2. SOLUCIÓN (by lazy): Obligamos a que la Base de Datos y la Autenticación
    // esperen a que Firebase esté listo antes de intentar conectarse.
    private val authManager by lazy { FirebaseAuthManager() }
    private val userManager by lazy { UserManager(FirebaseFirestore.getInstance()) }
    private val discordService by lazy { DiscordAuthService(context) }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState

    sealed class AuthState {
        object Loading : AuthState()
        object Unauthenticated : AuthState()
        object Guest : AuthState()
        data class Authenticated(val userId: String, val profile: UserProfile?) : AuthState()
    }

    // 3. Este init ahora es seguro porque se ejecuta DESPUÉS del escudo de Firebase
    init {
        if (authManager.currentUser != null) {
            viewModelScope.launch {
                val profile = userManager.getUserProfile(authManager.currentUser!!.uid)
                _authState.value = AuthState.Authenticated(authManager.currentUser!!.uid, profile)
            }
        }
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _errorMessage.value = null
            val result = authManager.signInWithEmail(email, password)
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                val profile = userManager.getUserProfile(user.uid)
                _authState.value = AuthState.Authenticated(user.uid, profile)
            } else {
                _errorMessage.value = "Error: ${result.exceptionOrNull()?.localizedMessage}"
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun registerWithEmail(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _errorMessage.value = null
            val result = authManager.signUpWithEmail(email, password, displayName)
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                val profile = UserProfile(
                    userId = user.uid,
                    email = email,
                    displayName = displayName,
                    photoUrl = user.photoUrl?.toString(),
                    discordId = null,
                    discordUsername = null
                )
                userManager.createOrUpdateUserProfile(user.uid, profile)
                _authState.value = AuthState.Authenticated(user.uid, profile)
            } else {
                _errorMessage.value = "Error al registrarse: ${result.exceptionOrNull()?.localizedMessage}"
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    suspend fun handleDiscordCallback(code: String): Boolean {
        return try {
            val tokenResult = discordService.exchangeCodeForToken(code)
            if (tokenResult.isFailure) {
                _errorMessage.value = "Error del Token de Discord: ${tokenResult.exceptionOrNull()?.message}"
                return false
            }
            val token = tokenResult.getOrNull()!!
            val userResult = discordService.fetchUserInfo(token)
            if (userResult.isFailure) {
                _errorMessage.value = "Error de Usuario de Discord: ${userResult.exceptionOrNull()?.message}"
                return false
            }
            val discordUser = userResult.getOrNull()!!

            val signInResult = authManager.signInWithDiscord(
                discordUser.id,
                discordUser.email ?: "", // <-- Operador Elvis para evitar el crasheo de nulos
                discordUser.username,
                "https://cdn.discordapp.com/avatars/${discordUser.id}/${discordUser.avatar}.png"
            )

            if (signInResult.isSuccess) {
                val firebaseUser = signInResult.getOrNull()!!
                val existingProfile = userManager.getUserProfile(firebaseUser.uid)
                val profile = if (existingProfile == null) {
                    val newProfile = UserProfile(
                        userId = firebaseUser.uid,
                        email = discordUser.email,
                        displayName = discordUser.username,
                        photoUrl = "https://cdn.discordapp.com/avatars/${discordUser.id}/${discordUser.avatar}.png",
                        discordId = discordUser.id,
                        discordUsername = discordUser.username
                    )
                    userManager.createOrUpdateUserProfile(firebaseUser.uid, newProfile)
                    newProfile
                } else {
                    existingProfile
                }
                _authState.value = AuthState.Authenticated(firebaseUser.uid, profile)
                true
            } else {
                _errorMessage.value = "Error de inicio de sesión de Discord en Firebase: ${signInResult.exceptionOrNull()?.message}"
                false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error inesperado: ${e.message}"
            false
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _errorMessage.value = null
            android.util.Log.i("GOOGLE_AUTH", "--- INICIO DE PROCESO DE AUTENTICACIÓN ---")

            try {
                val result = authManager.signInWithGoogle(idToken)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    if (user != null) {
                        try {
                            val existingProfile = userManager.getUserProfile(user.uid)
                            val profile = if (existingProfile == null) {
                                val newProfile = UserProfile(
                                    userId = user.uid,
                                    email = user.email,
                                    displayName = user.displayName ?: "Usuario de Google",
                                    photoUrl = user.photoUrl?.toString(),
                                    discordId = null,
                                    discordUsername = null
                                )
                                userManager.createOrUpdateUserProfile(user.uid, newProfile)
                                newProfile
                            } else {
                                existingProfile
                            }
                            _authState.value = AuthState.Authenticated(user.uid, profile)
                        } catch (e: Exception) {
                            android.util.Log.e("GOOGLE_AUTH", "Error de sincronización de perfil", e)
                            _errorMessage.value = "Error de sincronización: ${e.message}"
                            _authState.value = AuthState.Unauthenticated
                        }
                    } else {
                        _errorMessage.value = "Error: Usuario nulo tras autenticación"
                        _authState.value = AuthState.Unauthenticated
                    }
                } else {
                    val error = result.exceptionOrNull()
                    android.util.Log.e("GOOGLE_AUTH", "Error de autenticación", error)
                    _errorMessage.value = "Error de autenticación: ${error?.message ?: "Desconocido"}"
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                android.util.Log.e("GOOGLE_AUTH", "Error crítico en loginWithGoogle", e)
                _errorMessage.value = "Error crítico: ${e.message}"
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun loginAsGuest() {
        _errorMessage.value = null
        _authState.value = AuthState.Guest
    }

    fun loginWithUserId(userId: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _errorMessage.value = null
            try {
                val profile = userManager.getUserProfile(userId)
                if (profile != null) {
                    _authState.value = AuthState.Authenticated(userId, profile)
                } else {
                    _errorMessage.value = "No se encontró ningún perfil con ese ID"
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al buscar ID: ${e.message}"
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun logout() {
        try {
            authManager.signOut()
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Error al cerrar sesión", e)
        }
        _authState.value = AuthState.Unauthenticated
        _errorMessage.value = null
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    fun updateProfile(
        newName: String,
        newImageUri: Uri?,
        bio: String? = null,
        birthday: String? = null,
        mangaInterests: List<String> = emptyList()
    ) {
        val currentState = _authState.value
        if (currentState is AuthState.Authenticated) {
            viewModelScope.launch {
                _authState.value = AuthState.Loading
                _errorMessage.value = null
                
                try {
                    val userId = currentState.userId
                    var uploadedPhotoUrl: String? = currentState.profile?.photoUrl

                    // Subir nueva foto si existe
                    if (newImageUri != null) {
                        val newUrl = userManager.uploadProfileImage(userId, newImageUri)
                        if (newUrl != null) {
                            uploadedPhotoUrl = newUrl
                        } else {
                            _errorMessage.value = "Error al subir la imagen"
                            _authState.value = currentState // Restaurar estado
                            return@launch
                        }
                    }

                    // Actualizar el perfil en Firestore
                    userManager.updateUserProfile(userId, newName, uploadedPhotoUrl, bio, birthday, mangaInterests)
                    
                    // Actualizar el nombre en Firebase Auth (opcional, pero buena práctica)
                    try {
                        val updateRequest = com.google.firebase.auth.userProfileChangeRequest {
                            displayName = newName
                            if (uploadedPhotoUrl != null) {
                                photoUri = Uri.parse(uploadedPhotoUrl)
                            }
                        }
                        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.updateProfile(updateRequest)?.await()
                    } catch (e: Exception) {
                        android.util.Log.e("AuthViewModel", "Error updating Firebase Auth profile", e)
                    }

                    // Refrescar el perfil local
                    val updatedProfile = userManager.getUserProfile(userId)
                    _authState.value = AuthState.Authenticated(userId, updatedProfile)
                } catch (e: Exception) {
                    _errorMessage.value = "Error actualizando perfil: ${e.message}"
                    _authState.value = currentState // Restaurar estado
                }
            }
        }
    }
}