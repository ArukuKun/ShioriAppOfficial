package com.example.shioriapp

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class ShioriApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("ShioriApp", "Firebase initialized manually in Application class.")
            } else {
                Log.d("ShioriApp", "Firebase was already initialized automatically.")
            }
        } catch (e: Exception) {
            Log.e("ShioriApp", "Failed to initialize Firebase in Application class", e)
        }
    }
}
