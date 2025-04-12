package com.example.savvy.ui.tambah

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class TambahTransaksiViewModel @Inject constructor(
    val uploader: SupabaseStorageUploader,
    private val localTransactionDao: LocalTransactionDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // Fungsi untuk menyimpan transaksi (online atau offline)
    fun saveTransaction(
        transaction: Transaction,
        imageUri: Uri?,
        onSuccess: (String?) -> Unit, // Mengembalikan ID Firestore jika online
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (isNetworkAvailable(context)) {
                    // Jika online, simpan langsung ke Firestore
                    var imageUrl: String? = null
                    if (imageUri != null) {
                        val inputStream = context.contentResolver.openInputStream(imageUri)
                        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()

                        val byteArrayOutputStream = java.io.ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
                        val compressedByteArray = byteArrayOutputStream.toByteArray()

                        val file = File(context.cacheDir, "temp_image.jpg")
                        file.writeBytes(compressedByteArray)

                        val destinationFileName = "images/${System.currentTimeMillis()}_image.jpg"
                        imageUrl = withContext(Dispatchers.IO) {
                            uploader.uploadImage(file, destinationFileName)
                        }
                    }

                    val transactionToSave = transaction.copy(imageUrl = imageUrl)
                    val documentRef = db.collection("transactions").add(transactionToSave).await()
                    onSuccess(documentRef.id)
                } else {
                    // Jika offline, simpan ke Room
                    val localTransaction = LocalTransaction(
                        userId = transaction.userId,
                        type = transaction.type,
                        amount = transaction.amount,
                        category = transaction.category,
                        note = transaction.note,
                        date = transaction.date!!, // Pastikan date tidak null
                        imageUri = imageUri?.toString(),
                        isSynced = false
                    )
                    val localId = localTransactionDao.insert(localTransaction)
                    onSuccess(null)
                }
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    // Fungsi untuk memperbarui transaksi (online atau offline)
    fun updateTransaction(
        transactionId: String?, // ID Firestore (jika ada)
        localTransactionId: Long?, // ID Room (jika ada)
        transaction: Transaction,
        imageUri: Uri?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (isNetworkAvailable(context)) {
                    // Jika online, perbarui di Firestore
                    var imageUrl: String? = transaction.imageUrl
                    if (imageUri != null) {
                        val inputStream = context.contentResolver.openInputStream(imageUri)
                        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()

                        val byteArrayOutputStream = java.io.ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
                        val compressedByteArray = byteArrayOutputStream.toByteArray()

                        val file = File(context.cacheDir, "temp_image.jpg")
                        file.writeBytes(compressedByteArray)

                        val destinationFileName = "images/${System.currentTimeMillis()}_image.jpg"
                        imageUrl = withContext(Dispatchers.IO) {
                            uploader.uploadImage(file, destinationFileName)
                        }
                    }

                    val transactionToSave = transaction.copy(imageUrl = imageUrl)
                    if (transactionId != null) {
                        db.collection("transactions")
                            .document(transactionId)
                            .set(transactionToSave)
                            .await()
                    }

                    // Jika ada transaksi lokal, hapus dari Room
                    if (localTransactionId != null) {
                        localTransactionDao.deleteById(localTransactionId)
                    }

                    onSuccess()
                } else {
                    // Jika offline, perbarui di Room
                    if (localTransactionId != null) {
                        val localTransaction = LocalTransaction(
                            id = localTransactionId,
                            type = transaction.type,
                            amount = transaction.amount,
                            category = transaction.category,
                            note = transaction.note,
                            date = transaction.date!!, // Pastikan date tidak null
                            userId = transaction.userId,
                            imageUri = imageUri?.toString(),
                            isSynced = false
                        )
                        localTransactionDao.update(localTransaction)
                        onSuccess()
                    } else {
                        // Jika tidak ada localTransactionId, simpan sebagai transaksi lokal baru
                        val newLocalTransaction = LocalTransaction(
                            userId = transaction.userId,
                            type = transaction.type,
                            amount = transaction.amount,
                            category = transaction.category,
                            note = transaction.note,
                            date = transaction.date!!, // Pastikan date tidak null
                            imageUri = imageUri?.toString(),
                            isSynced = false
                        )
                        localTransactionDao.insert(newLocalTransaction)
                        onSuccess()
                    }
                }
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    // Fungsi untuk menyinkronkan transaksi lokal ke Firestore (dipanggil secara eksplisit)
    fun syncLocalTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            localTransactionDao.getUnsyncedTransactions().collect { unsyncedTransactions ->
                for (localTransaction in unsyncedTransactions) {
                    try {
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

                        if (localTransaction.imageUri != null) {
                            val imageUri = Uri.parse(localTransaction.imageUri)
                            val inputStream = context.contentResolver.openInputStream(imageUri)
                            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                            inputStream?.close()

                            val byteArrayOutputStream = java.io.ByteArrayOutputStream()
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
                            val compressedByteArray = byteArrayOutputStream.toByteArray()

                            val file = File(context.cacheDir, "temp_image.jpg")
                            file.writeBytes(compressedByteArray)

                            val destinationFileName = "images/${System.currentTimeMillis()}_image.jpg"
                            val imageUrl = withContext(Dispatchers.IO) {
                                uploader.uploadImage(file, destinationFileName)
                            }

                            if (imageUrl != null) {
                                documentReference.update("imageUrl", imageUrl).await()
                            }
                        }

                        // Hapus transaksi lokal setelah berhasil disinkronkan
                        localTransactionDao.deleteById(localTransaction.id)
                    } catch (e: Exception) {
                        // Jika gagal, transaksi tetap ada di Room untuk dicoba lagi nanti
                    }
                }
            }
        }
    }

    // Fungsi untuk memeriksa koneksi internet
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}