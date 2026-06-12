package com.example.shioriapp.auth

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class DiscordAuthService(private val context: Context) {
    private val client = OkHttpClient()
    private val gson = Gson()

    companion object {
        const val CLIENT_ID = "1511382251383029901"
        
        const val CLIENT_SECRET = "TU_CLIENT_SECRET_AQUI"
        
        const val REDIRECT_URI = "http://localhost/callback"
        
        const val AUTHORIZE_URL = "https://discord.com/api/oauth2/authorize" +
                "?client_id=$CLIENT_ID" +
                "&redirect_uri=$REDIRECT_URI" +
                "&response_type=code" +
                "&scope=identify%20email"

        private const val TOKEN_URL = "https://discord.com/api/oauth2/token"
        private const val USER_URL = "https://discord.com/api/users/@me"
    }

    suspend fun exchangeCodeForToken(code: String): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (CLIENT_SECRET == "TU_CLIENT_SECRET_AQUI") {
            return@withContext Result.failure(Exception("CLIENT_SECRET no configurado en DiscordAuthService.kt"))
        }

        val requestBody = "client_id=$CLIENT_ID" +
                "&client_secret=$CLIENT_SECRET" +
                "&grant_type=authorization_code" +
                "&code=$code" +
                "&redirect_uri=$REDIRECT_URI"
        
        val body = requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType())
        
        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(body)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        try {
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            Log.d("DiscordAuth", "Token Response: $bodyString")
            
            if (response.isSuccessful) {
                val tokenMap = gson.fromJson(bodyString, Map::class.java)
                val accessToken = tokenMap["access_token"] as? String
                if (accessToken != null) {
                    Result.success(accessToken)
                } else {
                    Result.failure(IOException("No access_token en la respuesta de Discord"))
                }
            } else {
                Result.failure(IOException("Error Discord (${response.code}): $bodyString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchUserInfo(accessToken: String): Result<DiscordUser> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val request = Request.Builder()
            .url(USER_URL)
            .header("Authorization", "Bearer $accessToken")
            .build()
        try {
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            Log.d("DiscordAuth", "User Info Response: $bodyString")
            
            if (response.isSuccessful) {
                val user = gson.fromJson(bodyString, DiscordUser::class.java)
                Result.success(user)
            } else {
                Result.failure(IOException("Error al obtener usuario (${response.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class DiscordUser(
        val id: String,
        val username: String,
        val discriminator: String,
        val avatar: String?,
        val email: String?
    )
}
