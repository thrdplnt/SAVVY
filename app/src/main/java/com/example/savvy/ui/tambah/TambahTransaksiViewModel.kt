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
import com.example.savvy.data.LocalTransaction // Pastikan import ini ada
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
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TambahTransaksiViewModel @Inject constructor(
    private val uploader: SupabaseStorageUploader,
    private val localTransactionDao: LocalTransactionDao, // Diubah menjadi private val
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    init {
        monitorNetworkStatus()
    }

    // Fungsi yang diminta untuk mengambil LocalTransaction berdasarkan ID Room-nya
    fun getLocalTransactionByDbId(localId: Long, onResult: (LocalTransaction?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { // Sebaiknya operasi DB di IO dispatcher
            val localTx = localTransactionDao.getTransactionByLocalId(localId)
            withContext(Dispatchers.Main) { // Kembali ke Main thread untuk callback UI
                onResult(localTx)
            }
        }
    }

    fun saveTransaction(
        transactionInput: Transaction,
        imageUri: Uri?,
        onSuccess: (String?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userId = transactionInput.userId
                if (userId.isBlank()) {
                    throw IllegalStateException("UserID cannot be blank for transaction.")
                }
                // Pastikan date tidak null, jika bisa null di TransactionInput, berikan default di sini
                val transactionDate = transactionInput.date ?: Date()

                var localImagePath: String? = null
                if (imageUri != null) {
                    val imageDir = File(context.filesDir, "transaction_images")
                    if (!imageDir.exists()) imageDir.mkdirs()
                    val file = File(imageDir, "local_image_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(imageUri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    localImagePath = file.absolutePath
                    Log.d("TambahVM", "Saved local image: $localImagePath")
                }

                val clientGeneratedId = UUID.randomUUID().toString()

                val localTransaction = LocalTransaction(
                    clientGeneratedId = clientGeneratedId,
                    userId = userId,
                    type = transactionInput.type,
                    amount = transactionInput.amount,
                    category = transactionInput.category,
                    note = transactionInput.note,
                    date = transactionDate,
                    imageUri = localImagePath,
                    imageUrl = null,
                    isSynced = false,
                    firestoreId = null,
                    walletId = transactionInput.walletId
                )
                val localDbId = localTransactionDao.insert(localTransaction)
                Log.d("TambahVM", "Inserted local TX (Room ID: $localDbId, ClientUUID: $clientGeneratedId)")

                if (withContext(Dispatchers.IO) { isNetworkAvailable(context) }) {
                    Log.d("TambahVM", "Online mode for ClientUUID: $clientGeneratedId")
                    var firestoreDocId: String? = null
                    var cloudImageUrl: String? = null

                    val existingDocs = db.collection("transactions")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("clientGeneratedId", clientGeneratedId)
                        .limit(1).get().await()

                    if (!existingDocs.isEmpty) {
                        val doc = existingDocs.documents[0]
                        firestoreDocId = doc.id
                        cloudImageUrl = doc.getString("imageUrl")
                        Log.i("TambahVM", "ClientUUID $clientGeneratedId already in Firestore (ID: $firestoreDocId).")
                        if (localImagePath != null && cloudImageUrl == null) {
                            val file = File(localImagePath)
                            if (file.exists()) {
                                val destName = "images/${System.currentTimeMillis()}_${file.name}"
                                val uploadedUrl = withContext(Dispatchers.IO) { uploader.uploadImage(file, destName) }
                                if (uploadedUrl != null) {
                                    cloudImageUrl = uploadedUrl
                                    db.collection("transactions").document(firestoreDocId)
                                        .update("imageUrl", cloudImageUrl).await()
                                }
                            }
                        }
                    } else {
                        Log.d("TambahVM", "ClientUUID $clientGeneratedId not in Firestore. Creating new doc.")
                        if (localImagePath != null) {
                            val file = File(localImagePath)
                            if (file.exists()) {
                                val destName = "images/${System.currentTimeMillis()}_${file.name}"
                                cloudImageUrl = withContext(Dispatchers.IO) { uploader.uploadImage(file, destName) }
                            }
                        }
                        val firestoreTx = Transaction(
                            clientGeneratedId = clientGeneratedId,
                            userId = userId,
                            type = transactionInput.type,
                            amount = transactionInput.amount,
                            category = transactionInput.category,
                            note = transactionInput.note,
                            date = transactionDate,
                            imageUrl = cloudImageUrl,
                            walletId = transactionInput.walletId,
                            imageUri = null
                        )
                        val docRef = db.collection("transactions").add(firestoreTx).await()
                        firestoreDocId = docRef.id
                        Log.i("TambahVM", "Saved to Firestore (ID: $firestoreDocId) for ClientUUID: $clientGeneratedId")
                    }

                    localTransactionDao.update(
                        localTransaction.copy(
                            id = localDbId,
                            isSynced = true,
                            firestoreId = firestoreDocId,
                            imageUrl = cloudImageUrl
                        )
                    )
                    Log.d("TambahVM", "Updated local TX (Room ID: $localDbId). Synced. Firestore ID: $firestoreDocId")
                    onSuccess(firestoreDocId)
                } else {
                    Log.d("TambahVM", "Offline mode. Saved to Room only (ClientUUID: $clientGeneratedId)")
                    onSuccess(null)
                }

            } catch (e: Exception) {
                Log.e("TambahVM", "Error saving transaction: $e", e)
                onFailure(e)
            }
        }
    }

    fun updateTransaction(
        existingFirestoreId: String?,
        existingLocalId: Long?,
        existingClientGeneratedId: String?,
        transactionInput: Transaction,
        newImageUri: Uri?,
        isImageRemoved: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("TambahVM-Update", "Updating TX. FirestoreID: $existingFirestoreId, LocalID: $existingLocalId, ClientUUID: $existingClientGeneratedId, ImgRemoved: $isImageRemoved")
                val userId = transactionInput.userId
                if (userId.isBlank()) throw IllegalStateException("UserID cannot be blank.")

                val transactionDate = transactionInput.date ?: Date() // Pastikan non-null

                var currentLocalImagePath: String? = null
                var finalCloudImageUrl: String? = transactionInput.imageUrl

                if (isImageRemoved) {
                    finalCloudImageUrl = null
                    // Hapus gambar lama dari Supabase jika ada existingImageUrlFromDb (perlu URL-nya)
                    // Logika penghapusan file Supabase bisa ditambahkan di sini jika diperlukan
                } else if (newImageUri != null) {
                    val imageDir = File(context.filesDir, "transaction_images")
                    if (!imageDir.exists()) imageDir.mkdirs()
                    val file = File(imageDir, "local_update_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(newImageUri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    currentLocalImagePath = file.absolutePath
                    Log.d("TambahVM-Update", "Saved new/updated local image: $currentLocalImagePath")
                }

                val clientGeneratedIdToUse = existingClientGeneratedId.takeIf { !it.isNullOrBlank() } ?: UUID.randomUUID().toString()
                var finalFirestoreIdForUpdate = existingFirestoreId

                if (withContext(Dispatchers.IO) { isNetworkAvailable(context) }) {
                    Log.d("TambahVM-Update", "Online mode for update.")
                    if (currentLocalImagePath != null) {
                        val file = File(currentLocalImagePath)
                        if (file.exists()) {
                            val destName = "images/update_${System.currentTimeMillis()}_${file.name}"
                            finalCloudImageUrl = withContext(Dispatchers.IO) { uploader.uploadImage(file, destName) }
                            Log.d("TambahVM-Update", "Uploaded updated image. Cloud URL: $finalCloudImageUrl")
                            // Jika gambar lama ada dan berbeda, mungkin perlu dihapus dari Supabase
                        }
                    }

                    val firestoreTxData = transactionInput.copy(
                        clientGeneratedId = clientGeneratedIdToUse,
                        imageUrl = finalCloudImageUrl,
                        imageUri = null,
                        date = transactionDate,
                        id = "" // id tidak dikirim untuk set/add di Firestore
                    )

                    if (!finalFirestoreIdForUpdate.isNullOrBlank()) {
                        db.collection("transactions").document(finalFirestoreIdForUpdate).set(firestoreTxData).await()
                        Log.i("TambahVM-Update", "Updated Firestore doc ID: $finalFirestoreIdForUpdate")
                    } else {
                        // Jika tidak ada firestoreId, coba cari berdasarkan clientGeneratedId
                        Log.w("TambahVM-Update", "existingFirestoreId is null. Checking ClientUUID $clientGeneratedIdToUse in Firestore.")
                        val querySnapshot = db.collection("transactions")
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("clientGeneratedId", clientGeneratedIdToUse)
                            .limit(1).get().await()

                        if(!querySnapshot.isEmpty) {
                            finalFirestoreIdForUpdate = querySnapshot.documents[0].id
                            db.collection("transactions").document(finalFirestoreIdForUpdate!!).set(firestoreTxData).await()
                            Log.i("TambahVM-Update", "Found and updated by clientUUID. Firestore doc ID: $finalFirestoreIdForUpdate")
                        } else {
                            // Jika benar-benar tidak ada, ini jadi seperti Save Baru (seharusnya jarang terjadi di flow update)
                            val newDocRef = db.collection("transactions").add(firestoreTxData).await()
                            finalFirestoreIdForUpdate = newDocRef.id
                            Log.i("TambahVM-Update", "No existing Firestore doc found by ID or ClientUUID. Created new: ${newDocRef.id}")
                        }
                    }
                }

                // Update atau Insert ke Room
                val localTxToUpdate = existingLocalId?.let { localTransactionDao.getTransactionByLocalId(it) }
                    ?: if (!finalFirestoreIdForUpdate.isNullOrBlank()) localTransactionDao.getByFirestoreId(finalFirestoreIdForUpdate)
                    else if (clientGeneratedIdToUse.isNotBlank()) localTransactionDao.getByClientGeneratedId(clientGeneratedIdToUse)
                    else null


                if (localTxToUpdate != null) {
                    localTransactionDao.update(
                        localTxToUpdate.copy(
                            type = transactionInput.type, amount = transactionInput.amount, category = transactionInput.category,
                            note = transactionInput.note, date = transactionDate, walletId = transactionInput.walletId,
                            imageUri = currentLocalImagePath ?: (if(isImageRemoved) null else localTxToUpdate.imageUri),
                            imageUrl = finalCloudImageUrl, // Update dengan URL cloud terbaru
                            isSynced = withContext(Dispatchers.IO) { isNetworkAvailable(context) }, // Set status sync
                            firestoreId = finalFirestoreIdForUpdate ?: localTxToUpdate.firestoreId, // Update firestoreId
                            clientGeneratedId = clientGeneratedIdToUse // Pastikan clientGeneratedId konsisten
                        )
                    )
                    Log.d("TambahVM-Update", "Updated local TX (Room ID: ${localTxToUpdate.id})")
                } else {
                    val newLocal = LocalTransaction(
                        clientGeneratedId = clientGeneratedIdToUse, userId = userId, type = transactionInput.type,
                        amount = transactionInput.amount, category = transactionInput.category, note = transactionInput.note,
                        date = transactionDate, imageUri = currentLocalImagePath, imageUrl = finalCloudImageUrl,
                        isSynced = withContext(Dispatchers.IO) { isNetworkAvailable(context) },
                        firestoreId = finalFirestoreIdForUpdate, walletId = transactionInput.walletId
                    )
                    localTransactionDao.insert(newLocal)
                    Log.d("TambahVM-Update", "No existing local TX found for update, inserted new one. Firestore ID: $finalFirestoreIdForUpdate")
                }
                onSuccess()

            } catch (e: Exception) {
                Log.e("TambahVM-Update", "Error updating transaction: $e", e)
                onFailure(e)
            }
        }
    }

    private suspend fun isNetworkAvailable(context: Context): Boolean {
        // ... (implementasi sama)
        return withTimeoutOrNull(1000L) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return@withTimeoutOrNull false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@withTimeoutOrNull false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } ?: false
    }

    private fun monitorNetworkStatus() {
        // ... (implementasi sama)
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("TambahTransaksiViewModel", "Network available")
            }
            override fun onLost(network: Network) { // Tambahkan onLost jika belum ada
                Log.d("TambahTransaksiViewModel", "Network lost")
            }
        }
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: SecurityException) {
            Log.e("TambahVM", "Network callback permission issue.", e)
        }
        viewModelScope.launch { // Pastikan ini ada jika belum
            viewModelScope.coroutineContext.job.invokeOnCompletion {
                try {
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                    Log.d("TambahVM", "Unregistered network callback")
                } catch (e: Exception) {
                    Log.w("TambahVM", "Error unregistering network callback: ${e.message}")
                }
            }
        }
    }
}