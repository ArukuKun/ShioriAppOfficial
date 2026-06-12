package com.example.shioriapp.viewmodel

import android.content.Context
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

class AuthViewModel(private val context: Context) : ViewModel() {

    // 1. ESCUDO: Si Firebase está apagado por culpa de la caché de Android, lo encendemos a la fuerza.
    init {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
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
            android.util.Log.i("GOOGLE_AUTH", "--- INICIO DE PROCESO DE AUTENTICACIÓN ---")

            val result = authManager.signInWithGoogle(idToken)
            if (result.isSuccess) {
                val user = result.getOrNull()!!

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
                    _errorMessage.value = "Error de sincronización: ${e.message}"
                    _authState.value = AuthState.Unauthenticated
                }
            } else {
                val error = result.exceptionOrNull()
                _errorMessage.value = "Error de autenticación Google: ${error?.message}"
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun loginAsGuest() {
        _authState.value = AuthState.Guest
    }

    fun logout() {
        authManager.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }
}