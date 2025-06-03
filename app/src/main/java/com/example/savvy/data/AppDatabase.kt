package com.example.savvy.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.savvy.data.converters.DateConverter

// Tambahkan LocalAnggaran ke entities dan naikkan versi database
@Database(entities = [LocalTransaction::class, LocalAnggaran::class], version = 5) // VERSI NAIK KE 5
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localTransactionDao(): LocalTransactionDao
    abstract fun anggaranDao(): AnggaranDao // Tambahkan DAO untuk Anggaran

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "savvy_database"
                )
                    // Karena versi database naik, Anda perlu strategi migrasi.
                    // fallbackToDestructiveMigration akan menghapus data lama dan membuat skema baru.
                    // Untuk produksi, Anda harus menyediakan objek Migration.
                    .fallbackToDestructiveMigration()
                    .build()
                // Log versi database yang diinisialisasi
                Log.d("AppDatabase", "Database initialized with version 5")
                INSTANCE = instance
                instance
            }
        }
    }
}