package com.example.shioriapp.core.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.shioriapp.domain.model.ChapterInfo
import java.io.OutputStream

object MangaDownloader {

    private fun getOrCreateMangaFolder(context: Context, treeUriStr: String, mangaTitle: String): DocumentFile? {
        if (treeUriStr.isBlank() || !treeUriStr.startsWith("content://")) return null
        return try {
            val rootFolder = DocumentFile.fromTreeUri(context, Uri.parse(treeUriStr)) ?: return null
            val cleanTitle = sanitizeFileName(mangaTitle)
            rootFolder.findFile(cleanTitle) ?: rootFolder.createDirectory(cleanTitle)
        } catch (e: Exception) {
            null
        }
    }

    fun isChapterDownloaded(context: Context, treeUriStr: String, mangaTitle: String, chapterName: String): Boolean {
        if (treeUriStr.isBlank()) return false
        return try {
            val mangaFolder = getOrCreateMangaFolder(context, treeUriStr, mangaTitle) ?: return false
            val cleanChapter = sanitizeFileName(chapterName)
            val chapterFolder = mangaFolder.findFile(cleanChapter)
            chapterFolder != null && chapterFolder.isDirectory && chapterFolder.listFiles().isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun downloadChapter(context: Context, treeUriStr: String, mangaTitle: String, chapter: ChapterInfo) {
        val mangaFolder = getOrCreateMangaFolder(context, treeUriStr, mangaTitle) ?: return
        val cleanChapter = sanitizeFileName(chapter.name)
        val chapterFolder = mangaFolder.findFile(cleanChapter) ?: mangaFolder.createDirectory(cleanChapter) ?: return

        for (i in 1..3) {
            val pageName = String.format("page_%03d.jpg", i)
            if (chapterFolder.findFile(pageName) == null) {
                val file = chapterFolder.createFile("image/jpeg", pageName)
                file?.let {
                    context.contentResolver.openOutputStream(it.uri)?.use { outputStream ->
                        outputStream.write("SHIORI_IMAGE_DATA".toByteArray()) // Simulación de bytes
                    }
                }
            }
        }
    }

    fun deleteChapter(context: Context, treeUriStr: String, mangaTitle: String, chapterName: String): Boolean {
        return try {
            val mangaFolder = getOrCreateMangaFolder(context, treeUriStr, mangaTitle) ?: return false
            val cleanChapter = sanitizeFileName(chapterName)
            val chapterFolder = mangaFolder.findFile(cleanChapter)
            chapterFolder?.delete() ?: true
        } catch (e: Exception) {
            false
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }
}