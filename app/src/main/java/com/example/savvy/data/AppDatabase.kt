package com.example.savvy.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.savvy.data.converters.DateConverter

@Database(entities = [LocalTransaction::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localTransactionDao(): LocalTransactionDao
}