package com.example.savvy

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.FirebaseApp

@HiltAndroidApp
class SavvyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            FirebaseApp.initializeApp(this@SavvyApp)
        }
        // Optional: Add initialization logic here
    }
}