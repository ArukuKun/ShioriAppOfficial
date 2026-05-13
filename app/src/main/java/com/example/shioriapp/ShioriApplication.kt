package com.example.shioriapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class ShioriApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("shiori_image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
}