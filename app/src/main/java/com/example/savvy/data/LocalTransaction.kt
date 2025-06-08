package com.example.savvy.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "local_transactions")
data class LocalTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientGeneratedId: String = UUID.randomUUID().toString(), // PASTIKAN INI ADA DAN MEMILIKI DEFAULT UUID
    val userId: String,
    val type: String,
    val amount: Long,
    val category: String,
    val note: String,
    val date: Date,
    val imageUri: String? = null,
    val imageUrl: String? = null,
    val isSynced: Boolean = false,
    val firestoreId: String? = null,
    val walletId: String? = ""
)