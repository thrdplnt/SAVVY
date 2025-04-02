package com.example.savvy.data

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val walletId: String = "",
    val amount: Long = 0,
    val category: String = "",
    val date: Long = 0,
    val type: String = "",
    val note: String = "",
    val receiptUrl: String? = null
)