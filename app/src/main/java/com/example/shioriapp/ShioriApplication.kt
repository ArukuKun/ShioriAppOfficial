package com.example.shioriapp

import android.app.Application
import com.google.firebase.FirebaseApp

class ShioriApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}