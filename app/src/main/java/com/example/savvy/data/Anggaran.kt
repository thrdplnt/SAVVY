package com.example.savvy.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Model untuk Firestore
data class Anggaran(
    val id: String = "", // Firestore Document ID
    val clientGeneratedId: String = "", // ID unik dari klien
    val userId: String = "",
    val name: String = "Anggaran", // Nama deskriptif untuk anggaran
    val category: String = "",
    val amount: Long = 0,
    @ServerTimestamp
    val startDate: Date? = null,
    @ServerTimestamp
    val endDate: Date? = null
)