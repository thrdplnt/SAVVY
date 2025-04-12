package com.example.savvy.di

import android.content.Context
import androidx.room.Room
import com.example.savvy.data.AppDatabase
import com.example.savvy.data.LocalTransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "savvy_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideLocalTransactionDao(database: AppDatabase): LocalTransactionDao {
        return database.localTransactionDao()
    }
}