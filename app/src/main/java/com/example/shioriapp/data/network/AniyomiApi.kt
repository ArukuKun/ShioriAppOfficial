package com.example.shioriapp.data.network

import com.example.shioriapp.domain.model.ExtensionInfo
import retrofit2.http.GET
import retrofit2.http.Url

interface AniyomiApi {
    @GET
    suspend fun getExtensions(@Url url: String): List<ExtensionInfo>
}