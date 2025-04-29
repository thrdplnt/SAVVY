package com.example.savvy.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.savvy.data.converters.DateConverter

@Database(entities = [LocalTransaction::class], version = 4) // Ubah version dari 1 ke 2
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localTransactionDao(): LocalTransactionDao

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
                    .fallbackToDestructiveMigration() // Tambahkan ini untuk mengizinkan migrasi destruktif
                    .build()
                Log.d("AppDatabase", "Database initialized with version 4")
                INSTANCE = instance
                instance
            }
        }
    }

}