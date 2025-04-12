package com.example.savvy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.savvy.ui.navigation.NavigationGraph
import com.example.savvy.ui.theme.SavvyTheme
import com.example.savvy.worker.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.view.WindowCompat
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mengatur window agar edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            SavvyTheme {
                NavigationGraph()
            }
        }

        // Jadwalkan sinkronisasi periodik
        scheduleSyncWorker()
    }

    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "sync_transactions",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
            )
    }
}