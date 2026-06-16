package com.example.shioriapp.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager {

    // SOLUCIÓN CLAVE: 'by lazy' hace que FirebaseAuth.getInstance()
    // no se ejecute cuando se crea la clase, sino solo cuando se va a usar
    // por primera vez (dándole tiempo a la app para inicializar Firebase).
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val currentUser: FirebaseUser?
        get() = try {
            auth.currentUser
        } catch (e: Exception) {
            null
        }

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuth", "Error signing out: ${e.message}")
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val res = auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(res.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, name: String): Result<FirebaseUser> {
        return try {
            val res = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = res.user!!
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            user.updateProfile(profileUpdates).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            if (authResult.user != null) {
                Result.success(authResult.user!!)
            } else {
                Result.failure(Exception("Fallo al iniciar sesión con Google"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithDiscord(
        discordId: String,
        email: String,
        username: String,
        avatarUrl: String
    ): Result<FirebaseUser> {
        return try {
            // Si no tienes backend, esta función de Discord fallará a nivel Firebase.
            Result.failure(Exception("El login directo con Discord requiere backend para Custom Token."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}