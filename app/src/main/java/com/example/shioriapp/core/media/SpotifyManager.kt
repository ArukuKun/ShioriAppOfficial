package com.example.shioriapp.core.media

import android.content.Context
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpotifyManager(private val context: Context) {

    // Tu Client ID
    private val clientId = "407e3f0bb456444394a565956ba1e39c"
    private val redirectUri = "shioriapp://callback"
    private var spotifyAppRemote: SpotifyAppRemote? = null

    // Estados que nuestra UI (Jetpack Compose) va a observar
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack = _currentTrack.asStateFlow()

    private val _isPaused = MutableStateFlow(true)
    val isPaused = _isPaused.asStateFlow()

    fun connect() {
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true) // Fuerza a abrir Spotify para pedir permiso la primera vez
            .build()

        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("SHIORI_SPOTIFY", "¡Conectado a Spotify!")
                subscribeToPlayerState()
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("SHIORI_SPOTIFY", "Error al conectar: ${throwable.message}")
            }
        })
    }

    private fun subscribeToPlayerState() {
        spotifyAppRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback { playerState ->
            _currentTrack.value = playerState.track
            _isPaused.value = playerState.isPaused
        }
    }

    // Funciones de control
    fun playPause() {
        if (_isPaused.value) {
            spotifyAppRemote?.playerApi?.resume()
        } else {
            spotifyAppRemote?.playerApi?.pause()
        }
    }

    fun skipNext() { spotifyAppRemote?.playerApi?.skipNext() }

    fun skipPrevious() { spotifyAppRemote?.playerApi?.skipPrevious() }

    fun disconnect() {
        SpotifyAppRemote.disconnect(spotifyAppRemote)
        spotifyAppRemote = null
    }

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()
}