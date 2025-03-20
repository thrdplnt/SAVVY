package com.example.savvy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(anime: AnimeEntity)

    @Query("SELECT * FROM anime_table WHERE malId = :id")
    suspend fun getAnimeById(id: Int): AnimeEntity?

    @Update
    suspend fun update(anime: AnimeEntity)

    @Query("SELECT * FROM anime_table WHERE isBookmarked = 1")
    fun getBookmarkedAnime(): Flow<List<AnimeEntity>>
}
