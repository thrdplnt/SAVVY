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
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject

@HiltViewModel
class TambahTransaksiViewModel @Inject constructor(
    private val uploader: SupabaseStorageUploader,
    val localTransactionDao: LocalTransactionDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    init {
        monitorNetworkStatus()
    }

    fun saveTransaction(
        transaction: Transaction,
        imageUri: Uri?,
        onSuccess: (String?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("TambahTransaksiViewModel", "Saving transaction: $transaction, imageUri: $imageUri")
                if (transaction.date == null) {
                    throw IllegalStateException("Transaction date cannot be null")
                }

                var localImagePath: String? = null
                if (imageUri != null) {
                    // Gunakan direktori files internal untuk penyimpanan persisten
                    val imageDir = File(context.filesDir, "transaction_images")
                    if (!imageDir.exists()) {
                        imageDir.mkdirs()
                        Log.d("TambahTransaksiViewModel", "Created directory: ${imageDir.absolutePath}")
                    }
                    val file = File(imageDir, "local_image_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(imageUri)?.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output, bufferSize = 8192)
                        }
                    }
                    localImagePath = file.absolutePath
                    if (file.exists()) {
                        Log.d("TambahTransaksiViewModel", "Saved local image at: $localImagePath, size: ${file.length()} bytes")
                    } else {
                        Log.w("TambahTransaksiViewModel", "Failed to save local image at: $localImagePath")
                    }
                }

                // Simpan ke Room terlebih dahulu
                val localTransaction = LocalTransaction(
                    userId = transaction.userId,
                    type = transaction.type,
                    amount = transaction.amount,
                    category = transaction.category,
                    note = transaction.note,
                    date = transaction.date,
                    imageUri = localImagePath,
                    imageUrl = null,
                    isSynced = false,
                    firestoreId = null,
                    walletId = transaction.walletId
                )
                val localId = localTransactionDao.insert(localTransaction)
                Log.d("TambahTransaksiViewModel", "Inserted local transaction with ID: $localId, imageUri: $localImagePath")

                if (withContext(Dispatchers.IO) { isNetworkAvailable(context) }) {
                    Log.d("TambahTransaksiViewModel", "Online mode: Saving to Firestore")

                    // Cek apakah transaksi sudah ada di Firestore
                    val existingFirestore = db.collection("transactions")
                        .whereEqualTo("userId", transaction.userId)
                        .whereEqualTo("type", transaction.type)
                        .whereEqualTo("amount", transaction.amount)
                        .whereEqualTo("category", transaction.category)
                        .whereEqualTo("note", transaction.note)
                        .whereEqualTo("date", transaction.date)
                        .get()
                        .await()
                        .documents
                        .firstOrNull()

                    if (existingFirestore != null) {
                        // Transaksi sudah ada, perbarui Room
                        val firestoreId = existingFirestore.id
                        val imageUrl = existingFirestore.toObject(Transaction::class.java)?.imageUrl
                        localTransactionDao.update(
                            localTransaction.copy(
                                id = localId,
                                isSynced = true,
                                firestoreId = firestoreId,
                                imageUrl = imageUrl
                            )
                        )
                        Log.d("TambahTransaksiViewModel", "Found duplicate in Firestore, updated local transaction ID: $localId")
                        onSuccess(firestoreId)
                        return@launch
                    }

                    // Unggah gambar jika ada
                    var imageUrl: String? = null
                    if (localImagePath != null) {
                        val file = File(localImagePath)
                        if (file.exists()) {
                            val destinationFileName = "images/${System.currentTimeMillis()}_image.jpg"
                            imageUrl = withContext(Dispatchers.IO) {
                                uploader.uploadImage(file, destinationFileName)
                            }
                            Log.d("TambahTransaksiViewModel", "Uploaded image URL: $imageUrl")
                        } else {
                            Log.w("TambahTransaksiViewModel", "Image file not found for upload: $localImagePath")
                        }
                    }

                    // Simpan ke Firestore
                    val transactionToSave = transaction.copy(imageUrl = imageUrl)
                    val documentRef = db.collection("transactions").add(transactionToSave).await()
                    Log.d("TambahTransaksiViewModel", "Saved to Firestore with ID: ${documentRef.id}")

                    // Perbarui Room dengan firestoreId dan imageUrl, pertahankan imageUri
                    localTransactionDao.update(
                        localTransaction.copy(
                            id = localId,
                            isSynced = true,
                            firestoreId = documentRef.id,
                            imageUrl = imageUrl
                        )
                    )
                    Log.d("TambahTransaksiViewModel", "Updated local transaction ID: $localId with Firestore ID: ${documentRef.id}, imageUri: $localImagePath")
                    onSuccess(documentRef.id)
                } else {
                    Log.d("TambahTransaksiViewModel", "Offline mode: Transaction saved to Room only, imageUri: $localImagePath")
                    onSuccess(null)
                }
            } catch (e: Exception) {
                Log.e("TambahTransaksiViewModel", "Error saving transaction: $e")
                onFailure(e)
            }
        }
    }

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

                var localImagePath: String? = null
                if (imageUri != null) {
                    // Gunakan direktori files internal untuk penyimpanan persisten
                    val imageDir = File(context.filesDir, "transaction_images")
                    if (!imageDir.exists()) {
                        imageDir.mkdirs()
                        Log.d("TambahTransaksiViewModel", "Created directory: ${imageDir.absolutePath}")
                    }
                    val file = File(imageDir, "local_image_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(imageUri)?.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output, bufferSize = 8192)
                        }
                    }
                    localImagePath = file.absolutePath
                    if (file.exists()) {
                        Log.d("TambahTransaksiViewModel", "Saved local image at: $localImagePath, size: ${file.length()} bytes")
                    } else {
                        Log.w("TambahTransaksiViewModel", "Failed to save local image at: $localImagePath")
                    }
                }

                if (withContext(Dispatchers.IO) { isNetworkAvailable(context) }) {
                    Log.d("TambahTransaksiViewModel", "Online mode: Updating Firestore")
                    var imageUrl: String? = transaction.imageUrl
                    if (imageUri != null && localImagePath != null) {
                        val file = File(localImagePath)
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

                    // Update atau simpan ke Room
                    if (localTransactionId != null) {
                        val localTransaction = LocalTransaction(
                            id = localTransactionId,
                            type = transaction.type,
                            amount = transaction.amount,
                            category = transaction.category,
                            note = transaction.note,
                            date = transaction.date,
                            userId = transaction.userId,
                            imageUri = localImagePath ?: transaction.imageUri,
                            imageUrl = imageUrl,
                            isSynced = true,
                            firestoreId = transactionId,
                            walletId = transaction.walletId
                        )
                        localTransactionDao.update(localTransaction)
                        Log.d("TambahTransaksiViewModel", "Updated local transaction ID: $localTransactionId, imageUri: ${localTransaction.imageUri}")
                    } else {
                        val localTransaction = LocalTransaction(
                            userId = transaction.userId,
                            type = transaction.type,
                            amount = transaction.amount,
                            category = transaction.category,
                            note = transaction.note,
                            date = transaction.date,
                            imageUri = localImagePath,
                            imageUrl = imageUrl,
                            isSynced = true,
                            firestoreId = transactionId,
                            walletId = transaction.walletId
                        )
                        localTransactionDao.insert(localTransaction)
                        Log.d("TambahTransaksiViewModel", "Inserted new local transaction for Firestore ID: $transactionId, imageUri: $localImagePath")
                    }
                    onSuccess()
                } else {
                    Log.d("TambahTransaksiViewModel", "Offline mode: Updating Room")
                    if (localTransactionId != null) {
                        val localTransaction = LocalTransaction(
                            id = localTransactionId,
                            type = transaction.type,
                            amount = transaction.amount,
                            category = transaction.category,
                            note = transaction.note,
                            date = transaction.date,
                            userId = transaction.userId,
                            imageUri = localImagePath ?: transaction.imageUri,
                            imageUrl = null,
                            isSynced = false,
                            firestoreId = transactionId,
                            walletId = transaction.walletId
                        )
                        localTransactionDao.update(localTransaction)
                        Log.d("TambahTransaksiViewModel", "Updated local transaction ID: $localTransactionId in offline mode, imageUri: ${localTransaction.imageUri}")
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
                            imageUrl = null,
                            isSynced = false,
                            firestoreId = null,
                            walletId = transaction.walletId
                        )
                        localTransactionDao.insert(newLocalTransaction)
                        Log.d("TambahTransaksiViewModel", "Inserted new local transaction in offline mode, imageUri: $localImagePath")
                        onSuccess()
                    }
                }
            } catch (e: Exception) {
                Log.e("TambahTransaksiViewModel", "Error updating transaction: $e")
                onFailure(e)
            }
        }
    }

    private suspend fun isNetworkAvailable(context: Context): Boolean {
        return withTimeoutOrNull(1000L) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return@withTimeoutOrNull false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@withTimeoutOrNull false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } ?: false
    }

    private fun monitorNetworkStatus() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("TambahTransaksiViewModel", "Network available")
                // Sinkronisasi ditangani oleh RiwayatViewModel
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        viewModelScope.launch {
            viewModelScope.coroutineContext.job.invokeOnCompletion {
                connectivityManager.unregisterNetworkCallback(networkCallback)
                Log.d("TambahTransaksiViewModel", "Unregistered network callback")
            }
        }
    }
}