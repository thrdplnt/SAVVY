package com.example.savvy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_user_profile")
data class LocalUser(
    @PrimaryKey val uid: String, // UID dari Firebase Auth akan menjadi Primary Key
    val displayName: String?,
    val email: String?,
    val photoUrl: String?, // URL gambar dari Supabase (setelah sinkronisasi)
    val localPhotoPath: String? = null, // Path ke file gambar di penyimpanan lokal perangkat
    val isSynced: Boolean = true // Untuk menandai jika ada perubahan yang belum diunggah
)
