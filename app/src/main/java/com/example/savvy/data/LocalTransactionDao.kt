package com.example.savvy.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalTransactionDao {
    @Insert
    suspend fun insert(localTransaction: LocalTransaction): Long

    @Update
    suspend fun update(localTransaction: LocalTransaction)

    @Query("DELETE FROM local_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM local_transactions WHERE isSynced = 0")
    fun getUnsyncedTransactions(): Flow<List<LocalTransaction>>

    @Query("SELECT * FROM local_transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactionsSync(): List<LocalTransaction>
}