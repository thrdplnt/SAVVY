package com.example.savvy.ui.transaction

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Transaction(
    val id: String = "", // ID dokumen di Firestore
    val type: String = "", // Jenis transaksi (Tunai/Non-Tunai)
    val amount: Long = 0, // Jumlah transaksi dalam Rp
    val category: String = "", // Kategori transaksi
    val note: String = "", // Catatan opsional
    @ServerTimestamp
    val date: Date? = null, // Tanggal transaksi (otomatis dari server)
    val userId: String = "", // ID pengguna
    val imageUrl: String? = null // URL gambar dari ImgBB
)