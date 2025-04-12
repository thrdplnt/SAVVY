package com.example.savvy.ui.tambah

import android.content.Context
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
import kotlinx.coroutines.flow.collectLatest
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

    init {
        // Mulai sinkronisasi saat ViewModel dibuat
        startSync()
    }

    private fun startSync() {
        viewModelScope.launch(Dispatchers.IO) {
            localTransactionDao.getUnsyncedTransactions().collectLatest { unsyncedTransactions ->
                for (localTransaction in unsyncedTransactions) {
                    syncTransaction(localTransaction)
                }
            }
        }
    }

    private suspend fun syncTransaction(localTransaction: LocalTransaction) {
        val transaction = Transaction(
            type = localTransaction.type,
            amount = localTransaction.amount,
            category = localTransaction.category,
            note = localTransaction.note,
            date = localTransaction.date,
            userId = localTransaction.userId,
            imageUrl = localTransaction.imageUrl
        )

        try {
            // Simpan ke Firestore menggunakan await()
            val documentReference = db.collection("transactions")
                .add(transaction)
                .await()

            // Jika ada imageUri, unggah gambar
            if (localTransaction.imageUri != null) {
                try {
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
                    // Jalankan uploadImage di dalam konteks IO
                    val imageUrl = withContext(Dispatchers.IO) {
                        uploader.uploadImage(file, destinationFileName)
                    }

                    if (imageUrl != null) {
                        // Perbarui dokumen Firestore dengan imageUrl
                        documentReference.update("imageUrl", imageUrl).await()
                    }
                } catch (e: Exception) {
                    // Jika gagal mengunggah gambar, biarkan imageUrl tetap null
                }
            }

            // Tandai sebagai disinkronkan dan hapus dari Room
            localTransactionDao.deleteById(localTransaction.id)
        } catch (e: Exception) {
            // Jika gagal menyimpan ke Firestore, transaksi tetap ada di Room untuk dicoba lagi nanti
        }
    }

    suspend fun saveTransactionLocally(
        type: String,
        amount: Long,
        category: String,
        note: String,
        date: java.util.Date,
        userId: String,
        imageUri: Uri?
    ): Long {
        val localTransaction = LocalTransaction(
            type = type,
            amount = amount,
            category = category,
            note = note,
            date = date,
            userId = userId,
            imageUri = imageUri?.toString()
        )
        return localTransactionDao.insert(localTransaction)
    }

    suspend fun updateTransactionLocally(
        transactionId: Long,
        type: String,
        amount: Long,
        category: String,
        note: String,
        date: java.util.Date,
        userId: String,
        imageUri: Uri?
    ) {
        val localTransaction = LocalTransaction(
            id = transactionId,
            type = type,
            amount = amount,
            category = category,
            note = note,
            date = date,
            userId = userId,
            imageUri = imageUri?.toString(),
            isSynced = false
        )
        localTransactionDao.update(localTransaction)
    }
}