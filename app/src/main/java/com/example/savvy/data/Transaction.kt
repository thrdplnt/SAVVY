package com.example.savvy.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val walletId: String = "",
    val type: String = "",
    val amount: Long = 0,
    val category: String = "",
    val note: String = "",
    @ServerTimestamp
    val date: Date? = null,
    val imageUrl: String? = null
)