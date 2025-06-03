package com.example.savvy.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "local_anggaran")
data class LocalAnggaran(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientGeneratedId: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val category: String,
    val amount: Long,
    val startDate: Date,
    val endDate: Date,
    val isSynced: Boolean = false,
    val firestoreId: String? = null
)