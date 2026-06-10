package com.example.shioriapp

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import coil.Coil
import coil.ImageLoader
import okhttp3.OkHttpClient
import com.example.shioriapp.core.util.ExtensionLoader
import com.example.shioriapp.navigation.AppNavigation
import com.example.shioriapp.ui.theme.ShioriAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ExtensionLoader.loadAllExtensions(this)

        enableEdgeToEdge()

        val imageLoader = ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val original = chain.request()
                        val cookies = CookieManager.getInstance().getCookie(original.url.toString()) ?: ""
                        val userAgent = WebSettings.getDefaultUserAgent(this@MainActivity)

                        val request = original.newBuilder()
                            .header("User-Agent", userAgent)
                            .header("Cookie", cookies)
                            .build()

                        chain.proceed(request)
                    }
                    .build()
            }
            .build()

        Coil.setImageLoader(imageLoader)

        setContent {
            ShioriAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}