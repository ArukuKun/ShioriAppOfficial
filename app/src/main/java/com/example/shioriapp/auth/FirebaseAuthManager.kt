package com.example.shioriapp.auth

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager {
    private val auth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(displayName).build())?.await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithDiscord(discordUserId: String, email: String?, displayName: String, photoUrl: String?): Result<FirebaseUser> {
        // Crear usuario en Firebase con email y contraseña aleatoria (o usar Custom Token)
        // Para simplicidad: si no existe, lo creamos con email = discordUserId@discord.local y password aleatoria
        val fakeEmail = email ?: "$discordUserId@discord.local"
        val password = "discord_$discordUserId"
        return try {
            val signInResult = auth.signInWithEmailAndPassword(fakeEmail, password).await()
            Result.success(signInResult.user!!)
        } catch (e: Exception) {
            // Usuario no existe, lo creamos
            try {
                val createResult = auth.createUserWithEmailAndPassword(fakeEmail, password).await()
                createResult.user?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(displayName).setPhotoUri(photoUrl?.let { Uri.parse(it) }).build())?.await()
                Result.success(createResult.user!!)
            } catch (e2: Exception) {
                Result.failure(e2)
            }
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}