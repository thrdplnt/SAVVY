package com.example.savvy

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SavvyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Optional: Add initialization logic here
    }
}