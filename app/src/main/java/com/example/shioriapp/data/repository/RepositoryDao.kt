package com.example.shioriapp.data.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RepositoryDao {
    // Trae toda la lista en tiempo real
    @Query("SELECT * FROM repositories")
    fun getAllRepositories(): Flow<List<RepositoryEntity>>

    // Guarda uno nuevo (o lo actualiza si ya existe)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepository(repo: RepositoryEntity)

    // Borra un repo
    @Delete
    suspend fun deleteRepository(repo: RepositoryEntity)
}