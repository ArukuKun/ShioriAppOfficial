package com.example.shioriapp.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ShioriFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        // Mostrar notificación local
        super.onMessageReceived(message)
    }
    override fun onNewToken(token: String) {
        // Guardar token en Firestore para el usuario actual
    }
}