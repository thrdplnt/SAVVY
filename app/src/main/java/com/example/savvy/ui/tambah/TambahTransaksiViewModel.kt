package com.example.savvy.ui.tambah

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvy.data.LocalTransaction
import com.example.savvy.data.LocalTransactionDao
import com.example.savvy.data.SupabaseStorageUploader
import com.example.savvy.data.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject

@HiltViewModel
class TambahTransaksiViewModel @Inject constructor(
    private val uploader: SupabaseStorageUploader,
    private val localTransactionDao: LocalTransactionDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    init {
        // Pantau status koneksi internet saat ViewModel diinisialisasi
        monitorNetworkStatus()
    }

    // Fungsi untuk menyimpan transaksi (online atau offline)
    fun saveTransaction(
        transaction: Transaction,
        imageUri: Uri?,
        onSuccess: (String?) -> Unit, // Mengembalikan ID Firestore jika online
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("TambahTransaksiViewModel", "Saving transaction: $transaction, imageUri: $imageUri")
                if (transaction.date == null) {
                    throw IllegalStateException("Transaction date cannot be null")
                }

                if (withContext(Dispatchers.IO) { isNetworkAvailable(context) }) {
                    Log.d("TambahTransaksiViewModel", "Online mode: Saving to Firestore")
                    var imageUrl: String? = null
                    if (imageUri != null) {
                        val startTime = System.currentTimeMillis()
                        val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                        context.contentResolver.openInputStream(imageUri)?.use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output, bufferSize = 8192)
                            }
                        }
                        val endTime = System.currentTimeMillis()
                        Log.d("TambahTransaksiViewModel", "Saved temp image to: ${file.absolutePath}, took ${endTime - startTime}ms")

                        val destinationFileName = "images/${System.currentTimeMillis()}_image.jpg"
                        imageUrl = withContext(Dispatchers.IO) {
                            uploader.uploadImage(file, destinationFileName)
                        }
                        Log.d("TambahTransaksiViewModel", "Uploaded image URL: $imageUrl")
                    }

                    val transactionToSave = transaction.copy(imageUrl = imageUrl)
                    val documentRef = db.collection("transactions").add(transactionToSave).await()
                    Log.d("TambahTransaksiViewModel", "Saved to Firestore with ID: ${documentRef.id}")

                    // Setelah menyimpan ke Firestore, coba sinkronkan transaksi lokal yang belum tersinkron
                    syncLocalTransactions()

                    onSuccess(documentRef.id)
                } else {
                    Log.d("TambahTransaksiViewModel", "Offline mode: Saving to Room")
                    var localImagePath: String? = null
                    if (imageUri != null) {
                        val startTime = System.currentTimeMillis()
                        val file = File(context.filesDir, "local_image_${System.currentTimeMillis()}.jpg")
                        context.contentResolver.openInputStream(imageUri)?.use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output, bufferSize = 8192)
                            }
                        }
                        localImagePath = file.absolutePath
                        val endTime = System.currentTimeMillis()
                        Log.d("TambahTransaksiViewModel", "Saved local image to: $localImagePath, took ${endTime - startTime}ms")
                    }

                    val localTransaction = LocalTransaction(
                        userId = transaction.userId,
                        type = transaction.type,
                        amount = transaction.amount,
                        category = transaction.category,
                        note = transaction.note,
                        date = transaction.date,
                        imageUri = localImagePath,
                        isSynced = false
                    )
                    val localId = localTransactionDao.insert(localTransaction)
                    Log.d("TambahTransaksiViewModel", "Inserted local transaction with ID: $localId")

                    // Verifikasi penyimpanan
                    val unsyncedTransactions = localTransactionDao.getUnsyncedTransactionsSync()
                    Log.d("TambahTransaksiViewModel", "Unsynced transactions after insert: $unsyncedTransactions")

                    onSuccess(null)
                }
            } catch (e: Exception) {
                Log.e("TambahTransaksiViewModel", "Error saving transaction: $e")
                onFailure(e)
            }
        }
    }

    // Fungsi untuk memperbarui transaksi (online atau offline)
    fun updateTransaction(
        transactionId: String?,
        localTransactionId: Long?,
        transaction: Transaction,
        imageUri: Uri?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("TambahTransaksiViewModel", "Updating transaction: $transaction, imageUri: $imageUri")
                if (transaction.date == null) {
                    throw IllegalStateException("Transaction date cannot be null")
                }

                if (withContext(Dispatchers.IO) { isNetworkAvailable(context) }) {
                    Log.d("TambahTransaksiViewModel", "Online mode: Updating Firestore")
                    var imageUrl: String? = transaction.imageUrl
                    if (imageUri != null) {
                        val startTime = System.currentTimeMillis()
                        val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                        context.contentResolver.openInputStream(imageUri)?.use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output, bufferSize = 8192)
                            }
                        }
                        val endTime = System.currentTimeMillis()
                        Log.d("TambahTransaksiViewModel", "Saved temp image to: ${file.absolutePath}, took ${endTime - startTime}ms")

                        val destinationFileName = "images/${System.currentTimeMillis()}_image.jpg"
                        imageUrl = withContext(Dispatchers.IO) {
                            uploader.uploadImage(file, destinationFileName)
                        }
                        Log.d("TambahTransaksiViewModel", "Uploaded updated image URL: $imageUrl")
                    }

                    val transactionToSave = transaction.copy(imageUrl = imageUrl)
                    if (transactionId != null) {
                        db.collection("transactions")
                            .document(transactionId)
                            .set(transactionToSave)
                            .await()
                        Log.d("TambahTransaksiViewModel", "Updated Firestore with ID: $transactionId")
                    }

                    // Jika ada transaksi lokal, hapus dari Room
                    if (localTransactionId != null) {
                        localTransactionDao.deleteById(localTransactionId)
                        Log.d("TambahTransaksiViewModel", "Deleted local transaction with ID: $localTransactionId")
                    }

                    // Setelah update, coba sinkronkan transaksi lokal yang belum tersinkron
                    syncLocalTransactions()

                    onSuccess()
                } else {
                    Log.d("TambahTransaksiViewModel", "Offline mode: Updating Room")
                    var localImagePath: String? = null
                    if (imageUri != null) {
                        val startTime = System.currentTimeMillis()
                        val file = File(context.filesDir, "local_image_${System.currentTimeMillis()}.jpg")
                        context.contentResolver.openInputStream(imageUri)?.use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output, bufferSize = 8192)
                            }
                        }
                        localImagePath = file.absolutePath
                        val endTime = System.currentTimeMillis()
                        Log.d("TambahTransaksiViewModel", "Saved local image to: $localImagePath, took ${endTime - startTime}ms")
                    }

                    if (localTransactionId != null) {
                        val localTransaction = LocalTransaction(
                            id = localTransactionId,
                            type = transaction.type,
                            amount = transaction.amount,
                            category = transaction.category,
                            note = transaction.note,
                            date = transaction.date,
                            userId = transaction.userId,
                            imageUri = localImagePath,
                            isSynced = false
                        )
                        localTransactionDao.update(localTransaction)
                        Log.d("TambahTransaksiViewModel", "Updated local transaction with ID: $localTransactionId")
                        onSuccess()
                    } else {
                        val newLocalTransaction = LocalTransaction(
                            userId = transaction.userId,
                            type = transaction.type,
                            amount = transaction.amount,
                            category = transaction.category,
                            note = transaction.note,
                            date = transaction.date,
                            imageUri = localImagePath,
                            isSynced = false
                        )
                        val newId = localTransactionDao.insert(newLocalTransaction)
                        Log.d("TambahTransaksiViewModel", "Inserted new local transaction with ID: $newId")
                        onSuccess()
                    }
                }
            } catch (e: Exception) {
                Log.e("TambahTransaksiViewModel", "Error updating transaction: $e")
                onFailure(e)
            }
        }
    }

    // Fungsi untuk menyinkronkan transaksi lokal ke Firestore
    fun syncLocalTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val unsyncedTransactions = localTransactionDao.getUnsyncedTransactionsSync()
                if (unsyncedTransactions.isEmpty()) {
                    Log.d("TambahTransaksiViewModel", "No unsynced transactions to sync")
                    return@launch
                }

                Log.d("TambahTransaksiViewModel", "Starting sync for ${unsyncedTransactions.size} unsynced transactions")
                for (localTransaction in unsyncedTransactions) {
                    try {
                        Log.d("TambahTransaksiViewModel", "Syncing transaction ID: ${localTransaction.id}")
                        val transaction = Transaction(
                            type = localTransaction.type,
                            amount = localTransaction.amount,
                            category = localTransaction.category,
                            note = localTransaction.note,
                            date = localTransaction.date,
                            userId = localTransaction.userId,
                            imageUrl = localTransaction.imageUrl
                        )

                        val documentReference = db.collection("transactions")
                            .add(transaction)
                            .await()
                        Log.d("TambahTransaksiViewModel", "Synced transaction to Firestore with ID: ${documentReference.id}")

                        if (localTransaction.imageUri != null) {
                            val file = File(localTransaction.imageUri)
                            if (file.exists()) {
                                val destinationFileName = "images/${System.currentTimeMillis()}_image.jpg"
                                val imageUrl = withContext(Dispatchers.IO) {
                                    uploader.uploadImage(file, destinationFileName)
                                }
                                if (imageUrl != null) {
                                    documentReference.update("imageUrl", imageUrl).await()
                                    Log.d("TambahTransaksiViewModel", "Updated Firestore with image URL: $imageUrl")
                                } else {
                                    Log.w("TambahTransaksiViewModel", "Failed to upload image for transaction ID: ${localTransaction.id}")
                                }
                            } else {
                                Log.w("TambahTransaksiViewModel", "Image file not found: ${localTransaction.imageUri}")
                            }
                        }

                        localTransactionDao.deleteById(localTransaction.id)
                        Log.d("TambahTransaksiViewModel", "Deleted synced local transaction with ID: ${localTransaction.id}")
                    } catch (e: Exception) {
                        Log.e("TambahTransaksiViewModel", "Failed to sync transaction ID ${localTransaction.id}: $e")
                        // Lanjutkan ke transaksi berikutnya, jangan hentikan proses
                    }
                }
                Log.d("TambahTransaksiViewModel", "Sync completed")
            } catch (e: Exception) {
                Log.e("TambahTransaksiViewModel", "Error during syncLocalTransactions: $e")
            }
        }
    }

    // Fungsi untuk memeriksa koneksi internet
    private suspend fun isNetworkAvailable(context: Context): Boolean {
        return withTimeoutOrNull(1000L) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return@withTimeoutOrNull false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@withTimeoutOrNull false
            val isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            Log.d("TambahTransaksiViewModel", "Network available: $isConnected")
            isConnected
        } ?: false
    }

    // Fungsi untuk memantau status koneksi internet
    private fun monitorNetworkStatus() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("TambahTransaksiViewModel", "Network available, attempting to sync local transactions")
                syncLocalTransactions()
            }

            override fun onLost(network: Network) {
                Log.d("TambahTransaksiViewModel", "Network lost")
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Unregister callback saat ViewModel dihancurkan
        viewModelScope.launch {
            onCleared {
                connectivityManager.unregisterNetworkCallback(networkCallback)
                Log.d("TambahTransaksiViewModel", "Unregistered network callback")
            }
        }
    }

    // Fungsi untuk menangani onCleared (opsional, untuk keamanan)
    private fun onCleared(block: () -> Unit) {
        viewModelScope.launch {
            block()
        }
    }
}