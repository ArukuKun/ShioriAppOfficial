package com.example.shioriapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.shioriapp.domain.model.ChapterInfo

class SharedReaderViewModel : ViewModel() {
    var chapters by mutableStateOf<List<ChapterInfo>>(emptyList())
    var currentChapter by mutableStateOf<ChapterInfo?>(null)
}