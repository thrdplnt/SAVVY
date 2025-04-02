package com.example.savvy.di

import android.content.Context
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
    fun provideSupabaseStorageUploader(@ApplicationContext context: Context): SupabaseStorageUploader {
        return SupabaseStorageUploader(context)
    }
}