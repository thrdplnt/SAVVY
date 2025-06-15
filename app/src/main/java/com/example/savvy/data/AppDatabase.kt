package com.example.savvy.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.savvy.data.converters.DateConverter

// Tambahkan LocalAnggaran ke entities dan naikkan versi database
@Database(entities = [LocalTransaction::class, LocalAnggaran::class, LocalUser::class], version = 6) // VERSI NAIK KE 5
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localTransactionDao(): LocalTransactionDao
    abstract fun anggaranDao(): AnggaranDao
    abstract fun userDao(): UserDao

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

                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}