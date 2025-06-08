package com.example.savvy.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import java.util.UUID


data class Transaction(
    val id: String = "", // Akan diisi Firestore Document ID
    val clientGeneratedId: String = UUID.randomUUID().toString(), // PASTIKAN INI NON-NULLABLE DAN DEFAULT KOSONG
    val userId: String = "",
    val walletId: String = "",
    val type: String = "", // Ini adalah tipe/nama dompet
    val amount: Long = 0,
    val category: String = "",
    val note: String = "",
    @ServerTimestamp
    val date: Date? = null,
    val imageUrl: String? = null,
    val imageUri: String? = null // Hanya untuk tampilan, tidak disimpan di Firestore
)