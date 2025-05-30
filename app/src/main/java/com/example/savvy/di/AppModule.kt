package com.example.savvy.di

import android.content.Context
import androidx.room.Room
import com.example.savvy.data.AppDatabase
import com.example.savvy.data.AppRepository
import com.example.savvy.data.LocalTransactionDao
import com.example.savvy.data.SupabaseStorageUploader
import com.example.savvy.ui.riwayat.RiwayatViewModel
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
    fun provideSupabaseStorageUploader(@ApplicationContext context: Context): SupabaseStorageUploader {
        return SupabaseStorageUploader(context)
    }

    @Provides
    @Singleton
    fun provideAppRepository(localTransactionDao: LocalTransactionDao): AppRepository {
        return AppRepository(localTransactionDao)
    }

//    @Provides
//    @Singleton
//    fun provideRiwayatViewModel(
//        localTransactionDao: LocalTransactionDao,
//        uploader: SupabaseStorageUploader,
//        appRepository: AppRepository,
//        @ApplicationContext context: Context
//    ): RiwayatViewModel {
//        return RiwayatViewModel(localTransactionDao, uploader, appRepository, context)
//    }
}