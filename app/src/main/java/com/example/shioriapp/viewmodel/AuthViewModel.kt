package com.example.shioriapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shioriapp.auth.DiscordAuthService
import com.example.shioriapp.auth.FirebaseAuthManager
import com.example.shioriapp.auth.UserManager
import com.example.shioriapp.domain.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val context: Context) : ViewModel() {
    private val authManager = FirebaseAuthManager()
    private val userManager = UserManager(FirebaseFirestore.getInstance())
    private val discordService = DiscordAuthService(context)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState

    sealed class AuthState {
        object Loading : AuthState()
        object Unauthenticated : AuthState()
        data class Authenticated(val userId: String, val profile: UserProfile?) : AuthState()
    }

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
                _errorMessage.value = "Discord Token Error: ${tokenResult.exceptionOrNull()?.message}"
                return false
            }
            val token = tokenResult.getOrNull()!!
            val userResult = discordService.fetchUserInfo(token)
            if (userResult.isFailure) {
                _errorMessage.value = "Discord User Error: ${userResult.exceptionOrNull()?.message}"
                return false
            }
            val discordUser = userResult.getOrNull()!!
            
            val signInResult = authManager.signInWithDiscord(
                discordUser.id,
                discordUser.email,
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
                _errorMessage.value = "Firebase Discord Sign-in Error: ${signInResult.exceptionOrNull()?.message}"
                false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Unexpected error: ${e.message}"
            false
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            android.util.Log.i("GOOGLE_AUTH", "--- INICIO DE PROCESO DE AUTENTICACIÓN ---")
            android.util.Log.d("GOOGLE_AUTH", "Paso 1: Intercambiando Token con Firebase...")
            
            val result = authManager.signInWithGoogle(idToken)
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                android.util.Log.i("GOOGLE_AUTH", "Paso 2: Autenticación Firebase EXITOSA. UID: ${user.uid}")
                
                try {
                    android.util.Log.d("GOOGLE_AUTH", "Paso 3: Verificando perfil en Firestore...")
                    val existingProfile = userManager.getUserProfile(user.uid)
                    
                    val profile = if (existingProfile == null) {
                        android.util.Log.w("GOOGLE_AUTH", "Aviso: No existe perfil. Creando nuevo registro...")
                        val newProfile = UserProfile(
                            userId = user.uid,
                            email = user.email,
                            displayName = user.displayName ?: "Usuario de Google",
                            photoUrl = user.photoUrl?.toString(),
                            discordId = null,
                            discordUsername = null
                        )
                        userManager.createOrUpdateUserProfile(user.uid, newProfile)
                        android.util.Log.i("GOOGLE_AUTH", "Perfil creado satisfactoriamente.")
                        newProfile
                    } else {
                        android.util.Log.i("GOOGLE_AUTH", "Perfil existente cargado correctamente.")
                        existingProfile
                    }
                    
                    _authState.value = AuthState.Authenticated(user.uid, profile)
                    android.util.Log.i("GOOGLE_AUTH", "--- PROCESO FINALIZADO CON ÉXITO ---")
                } catch (e: Exception) {
                    android.util.Log.e("GOOGLE_AUTH", "ERROR CRÍTICO en sincronización de Firestore", e)
                    _errorMessage.value = "Error de sincronización: ${e.message}"
                    _authState.value = AuthState.Unauthenticated
                }
            } else {
                val error = result.exceptionOrNull()
                android.util.Log.e("GOOGLE_AUTH", "ERROR en Firebase Auth: ${error?.message}")
                _errorMessage.value = "Error de autenticación Google: ${error?.message}"
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun logout() {
        authManager.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }
}