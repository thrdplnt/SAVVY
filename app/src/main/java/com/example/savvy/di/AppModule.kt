package com.example.savvy.di

import android.content.Context
import androidx.room.Room
import com.example.savvy.data.*
import com.example.savvy.data.AnggaranDao
import com.example.savvy.data.AppDatabase
import com.example.savvy.data.AppRepository
import com.example.savvy.data.LocalTransactionDao
import com.example.savvy.data.SupabaseStorageUploader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "savvy_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideLocalTransactionDao(database: AppDatabase): LocalTransactionDao {
        return database.localTransactionDao()
    }

    @Provides
    @Singleton
    fun provideAnggaranDao(database: AppDatabase): AnggaranDao {
        return database.anggaranDao()
    }
    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideSupabaseStorageUploader(@ApplicationContext context: Context): SupabaseStorageUploader {
        return SupabaseStorageUploader(context)
    }

    @Provides
    @Singleton
    fun provideAppRepository(
        localTransactionDao: LocalTransactionDao,
        anggaranDao: AnggaranDao,
        userDao: UserDao
    ): AppRepository {
        return AppRepository(localTransactionDao, anggaranDao, userDao) // <-- Pass AnggaranDao
    }
}