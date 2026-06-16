package com.example.shioriapp.data.repository

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repositories")
data class RepositoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val isDefault: Boolean = false
)