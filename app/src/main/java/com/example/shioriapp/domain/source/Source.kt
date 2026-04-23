package com.example.shioriapp.domain.source

import com.example.shioriapp.domain.model.ChapterInfo
import com.example.shioriapp.domain.model.MangaInfo
import com.example.shioriapp.domain.model.PageInfo

interface Source {
    val name: String
    val lang: String
    val id: Long

    suspend fun fetchSearchManga(query: String, page: Int): List<MangaInfo>
    suspend fun fetchMangaDetails(manga: MangaInfo): MangaInfo
    suspend fun fetchChapterList(manga: MangaInfo): List<ChapterInfo>
    suspend fun fetchPageList(chapter: ChapterInfo): List<PageInfo>
}