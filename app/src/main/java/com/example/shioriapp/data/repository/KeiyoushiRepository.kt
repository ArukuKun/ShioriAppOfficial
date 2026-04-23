package com.example.shioriapp.data.repository

import com.example.shioriapp.domain.model.ExtensionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class KeiyoushiRepository {

    private val indexUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json"

    suspend fun fetchExtensions(): List<ExtensionInfo> = withContext(Dispatchers.IO) {
        val extensionList = mutableListOf<ExtensionInfo>()

        try {
            val url = URL(indexUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                // Leemos todo el texto del archivo JSON
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }

                val jsonArray = JSONArray(responseString)

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)

                    val extension = ExtensionInfo(
                        name = jsonObject.getString("name"),
                        pkg = jsonObject.getString("pkg"),
                        apk = jsonObject.getString("apk"),
                        lang = jsonObject.getString("lang"),
                        code = jsonObject.getLong("code"),
                        version = jsonObject.getString("version"),
                        nsfw = jsonObject.getInt("nsfw")
                    )
                    extensionList.add(extension)
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext extensionList
    }
}