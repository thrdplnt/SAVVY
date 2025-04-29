//package com.example.savvy.di
//
//import android.content.Context
//import androidx.room.Room
//import com.example.savvy.data.AppDatabase
//import com.example.savvy.data.LocalTransactionDao
//import com.example.savvy.ui.riwayat.RiwayatViewModel
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.android.qualifiers.ApplicationContext
//import dagger.hilt.components.SingletonComponent
//import javax.inject.Singleton
//
//@Module
//@InstallIn(SingletonComponent::class)
//object ViewModelModule {
//    @Provides
//    @Singleton
//    fun provideRiwayatViewModel(
//        localTransactionDao: LocalTransactionDao,
//        @ApplicationContext context: Context
//    ): RiwayatViewModel {
//        return RiwayatViewModel(localTransactionDao, context)
//    }
//}