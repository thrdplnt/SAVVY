package com.example.savvy.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.savvy.ui.tambah.TambahTransaksiViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val viewModel: TambahTransaksiViewModel
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Starting sync worker")
        return try {
            viewModel.syncLocalTransactions()
            Log.d("SyncWorker", "Sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed: $e")
            Result.retry()
        }
    }
}