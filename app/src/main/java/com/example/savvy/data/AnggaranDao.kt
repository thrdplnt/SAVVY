package com.example.savvy.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface AnggaranDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(localAnggaran: LocalAnggaran): Long

    @Update
    suspend fun update(localAnggaran: LocalAnggaran)

    @Query("DELETE FROM local_anggaran WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM local_anggaran WHERE clientGeneratedId = :clientGeneratedId")
    suspend fun deleteByClientGeneratedId(clientGeneratedId: String)


    @Query("SELECT * FROM local_anggaran WHERE userId = :userId ORDER BY startDate DESC")
    fun getAllAnggaran(userId: String): Flow<List<LocalAnggaran>>

    @Query("SELECT * FROM local_anggaran WHERE id = :id")
    suspend fun getAnggaranByLocalId(id: Long): LocalAnggaran?

    @Query("SELECT * FROM local_anggaran WHERE clientGeneratedId = :clientGeneratedId LIMIT 1")
    suspend fun getAnggaranByClientGeneratedId(clientGeneratedId: String): LocalAnggaran?

    @Query("SELECT * FROM local_anggaran WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getAnggaranByFirestoreId(firestoreId: String): LocalAnggaran?

    @Query("SELECT * FROM local_anggaran WHERE userId = :userId AND category = :category AND :date BETWEEN startDate AND endDate")
    fun getActiveAnggaranForCategory(userId: String, category: String, date: Date): Flow<List<LocalAnggaran>>

    @Query("SELECT * FROM local_anggaran WHERE isSynced = 0")
    suspend fun getUnsyncedAnggaran(): List<LocalAnggaran>
}