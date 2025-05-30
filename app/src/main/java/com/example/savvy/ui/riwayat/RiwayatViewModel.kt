package com.example.savvy.ui.riwayat

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvy.data.AppRepository
import com.example.savvy.data.LocalTransactionDao
import com.example.savvy.data.SupabaseStorageUploader
import com.example.savvy.data.Transaction // Pastikan Transaction diimpor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date // Pastikan Date diimpor
import java.util.UUID // Pastikan UUID diimpor
import javax.inject.Inject

@HiltViewModel
class RiwayatViewModel @Inject constructor(
    private val localTransactionDao: LocalTransactionDao,
    private val uploader: SupabaseStorageUploader,
    private val appRepository: AppRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        Log.d("RiwayatViewModel", "ViewModel initialized")
        monitorNetworkStatus()
        viewModelScope.launch {
            Log.d("RiwayatViewModel", "Calling onUserLogin from init")
            appRepository.onUserLogin()
        }
    }

    private val localTransactionsFlow: Flow<List<Transaction>> = auth.currentUser?.uid?.let { userId ->
        localTransactionDao.getAllTransactions(userId)
            .map { localTransactionList ->
                localTransactionList.map { local -> // local adalah LocalTransaction
                    Transaction(
                        id = local.firestoreId ?: "local_${local.id}", // Ini adalah ID untuk UI
                        clientGeneratedId = local.clientGeneratedId,    // Ambil dari LocalTransaction
                        userId = local.userId,
                        type = local.type,
                        amount = local.amount,
                        category = local.category,
                        note = local.note,
                        date = local.date,
                        imageUrl = local.imageUrl,
                        imageUri = local.imageUri,
                        walletId = local.walletId ?: local.type // Fallback jika walletId null
                    )
                }
            }
    } ?: flowOf(emptyList<Transaction>())

    private val firestoreTransactionsFlow: Flow<List<Transaction>> = flow {
        val user = auth.currentUser
        if (user == null) { /* ... emit empty ... */ return@flow }
        try {
            val querySnapshot = db.collection("transactions")
                .whereEqualTo("userId", user.uid)
                .get()
                .await()
            val firestoreList = querySnapshot.documents.mapNotNull { doc ->
                // Penting: Bagaimana Transaction dibuat dari dokumen Firestore
                val data = doc.data
                if (data == null) return@mapNotNull null
                Transaction(
                    id = doc.id,
                    clientGeneratedId = data["clientGeneratedId"] as? String, // Bisa null
                    userId = data["userId"] as? String ?: user.uid,
                    walletId = data["walletId"] as? String ?: (data["type"] as? String ?: ""),
                    type = data["type"] as? String ?: "",
                    amount = data["amount"] as? Long ?: 0L,
                    category = data["category"] as? String ?: "",
                    note = data["note"] as? String ?: "",
                    date = (data["date"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                    imageUrl = data["imageUrl"] as? String,
                    imageUri = null
                )
            }
            emit(firestoreList)
        } catch (e: Exception) { /* ... log error, emit empty ... */ }
    }

    val transactions: StateFlow<List<Transaction>> = combine(
        localTransactionsFlow,
        firestoreTransactionsFlow
    ) { localTxsFromRoom, firestoreTxs ->
        Log.i("RiwayatVM-Combine", "Combining: ${localTxsFromRoom.size} local, ${firestoreTxs.size} Firestore.")
        val transactionMap = mutableMapOf<String, Transaction>() // Kunci bisa clientGeneratedId atau firestoreId

        // 1. Proses Firestore transactions, prioritaskan clientGeneratedId sebagai kunci jika ada
        firestoreTxs.forEach { firestoreTx ->
            val key = firestoreTx.clientGeneratedId.takeIf { !it.isNullOrBlank() } ?: firestoreTx.id
            if (key.isNotBlank()) {
                transactionMap[key] = firestoreTx
            } else {
                Log.w("RiwayatVM-Combine", "Firestore TX has blank key (clientGenId & id): $firestoreTx")
            }
        }

        // 2. Proses local transactions, coba cocokkan dengan yang sudah ada di map
        localTxsFromRoom.forEach { mappedLocalTx ->
            // mappedLocalTx.clientGeneratedId dari Room dijamin tidak blank karena default UUID
            val key = mappedLocalTx.clientGeneratedId!!

            val existingInMap = transactionMap[key] // Cari berdasarkan clientGeneratedId
            if (existingInMap != null) {
                // Ditemukan! Artinya transaksi ini sudah ada di Firestore.
                // Kita gunakan versi Firestore (existingInMap) sebagai basis,
                // tapi kita perbarui dengan info lokal yang mungkin lebih relevan (seperti imageUri).
                // Pastikan ID yang digunakan adalah Firestore ID.
                transactionMap[key] = existingInMap.copy(
                    id = existingInMap.id, // Jaga Firestore ID
                    imageUri = mappedLocalTx.imageUri ?: existingInMap.imageUri,
                    // Jika ada field lain yang mungkin berbeda dan ingin diprioritaskan dari lokal:
                    // note = mappedLocalTx.note, // contoh
                    // date = mappedLocalTx.date, // contoh
                    // imageUrl bisa jadi dari mappedLocalTx jika lebih baru (misal baru diupload dan belum ter-reflect di firestoreTxs)
                    imageUrl = mappedLocalTx.imageUrl ?: existingInMap.imageUrl
                )
            } else {
                // Tidak ditemukan di map berdasarkan clientGeneratedId.
                // Berarti ini transaksi yang hanya ada di lokal (belum sinkron atau gagal sinkron)
                // atau transaksi dari Firestore yang clientGeneratedId-nya tidak cocok/kosong.
                // Gunakan mappedLocalTx.id (yang bisa "local_..." atau firestoreId jika sudah di-link parsial) sebagai fallback key.
                transactionMap[mappedLocalTx.id] = mappedLocalTx
            }
        }
        transactionMap.values.sortedByDescending { it.date }
    }.catch { e ->
        Log.e("RiwayatViewModel", "Combine operator error: $e", e)
        emit(emptyList<Transaction>())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList<Transaction>()
    )

    private suspend fun syncLocalTransactions() {
        val user = auth.currentUser ?: run {
            Log.w("RiwayatVM-Sync", "User null, cannot sync.")
            return
        }
        val unsyncedTransactions = try {
            localTransactionDao.getUnsyncedTransactionsSync()
        } catch (e: Exception) {
            Log.e("RiwayatVM-Sync", "Error fetching unsynced: $e", e)
            emptyList()
        }

        if (unsyncedTransactions.isEmpty()) {
            Log.d("RiwayatVM-Sync", "No unsynced transactions.")
            return
        }
        Log.i("RiwayatVM-Sync", "Found ${unsyncedTransactions.size} unsynced transactions.")

        for (localTx in unsyncedTransactions) {
            if (localTx.isSynced && !localTx.firestoreId.isNullOrBlank()) {
                Log.d("RiwayatVM-Sync", "LocalTX ID ${localTx.id} (ClientUUID ${localTx.clientGeneratedId}) already marked synced with FirestoreID ${localTx.firestoreId}. Skipping.")
                continue
            }
            Log.d("RiwayatVM-Sync", "Processing LocalTX ID ${localTx.id}, ClientUUID ${localTx.clientGeneratedId}, FirestoreID: ${localTx.firestoreId}, Synced: ${localTx.isSynced}")

            try {
                var finalFirestoreId = localTx.firestoreId
                var cloudImageUrl = localTx.imageUrl

                // 1. Cek apakah transaksi dengan clientGeneratedId ini sudah ada di Firestore
                if (localTx.clientGeneratedId.isNotBlank()) { // Hanya cek jika clientGeneratedId ada
                    val existingDocs = db.collection("transactions")
                        .whereEqualTo("userId", user.uid)
                        .whereEqualTo("clientGeneratedId", localTx.clientGeneratedId)
                        .limit(1)
                        .get()
                        .await()

                    if (!existingDocs.isEmpty) {
                        val doc = existingDocs.documents[0]
                        finalFirestoreId = doc.id // Dapatkan firestoreId yang sudah ada
                        if (cloudImageUrl.isNullOrBlank()){ // Jika lokal tidak punya cloudImageUrl
                            cloudImageUrl = doc.getString("imageUrl") // Coba ambil dari Firestore
                        }
                        Log.i("RiwayatVM-Sync", "Found existing Firestore (ID: $finalFirestoreId) for ClientUUID ${localTx.clientGeneratedId} of LocalTX ${localTx.id}.")
                    }
                }


                // 2. Unggah gambar jika ada URI lokal dan belum ada URL cloud (atau URL cloud tidak valid)
                if (localTx.imageUri != null && cloudImageUrl.isNullOrBlank()) {
                    val imageFile = File(localTx.imageUri)
                    if (imageFile.exists()) {
                        val destinationFileName = "images/${System.currentTimeMillis()}_${imageFile.name}"
                        Log.d("RiwayatVM-Sync", "Uploading image for LocalTX ID ${localTx.id} (Path: ${localTx.imageUri})")
                        cloudImageUrl = withContext(Dispatchers.IO) {
                            uploader.uploadImage(imageFile, destinationFileName)
                        }
                        Log.d("RiwayatVM-Sync", "Uploaded image for LocalTX ID ${localTx.id}. Cloud URL: $cloudImageUrl")
                    } else {
                        Log.w("RiwayatVM-Sync", "Local image file not found for LocalTX ID ${localTx.id}: ${localTx.imageUri}")
                    }
                }

                // 3. Siapkan data untuk disimpan/diupdate ke Firestore
                val transactionForFirestore = Transaction(
                    clientGeneratedId = localTx.clientGeneratedId, // WAJIB ADA
                    userId = user.uid,
                    type = localTx.type,
                    amount = localTx.amount,
                    category = localTx.category,
                    note = localTx.note,
                    date = localTx.date,
                    imageUrl = cloudImageUrl,
                    walletId = localTx.walletId ?: "",
                    imageUri = null
                )

                if (finalFirestoreId.isNullOrBlank()) {
                    // Buat dokumen BARU di Firestore karena tidak ditemukan berdasarkan clientGeneratedId atau firestoreId lokal
                    Log.d("RiwayatVM-Sync", "Creating new Firestore doc for LocalTX ID ${localTx.id} (ClientUUID ${localTx.clientGeneratedId}).")
                    val documentReference = db.collection("transactions").add(transactionForFirestore).await()
                    finalFirestoreId = documentReference.id
                    Log.i("RiwayatVM-Sync", "Created new Firestore doc (ID: $finalFirestoreId) for LocalTX ID ${localTx.id}.")
                } else {
                    // UPDATE dokumen yang sudah ada di Firestore
                    Log.d("RiwayatVM-Sync", "Updating existing Firestore doc (ID: $finalFirestoreId) for LocalTX ID ${localTx.id}.")
                    db.collection("transactions").document(finalFirestoreId).set(transactionForFirestore).await()
                    Log.i("RiwayatVM-Sync", "Updated Firestore doc (ID: $finalFirestoreId).")
                }

                // 4. Update LocalTransaction di Room
                localTransactionDao.update(
                    localTx.copy(
                        isSynced = true,
                        firestoreId = finalFirestoreId,
                        imageUrl = cloudImageUrl
                    )
                )
                Log.d("RiwayatVM-Sync", "Marked LocalTX ID ${localTx.id} as synced. Firestore ID: $finalFirestoreId, Cloud Img: $cloudImageUrl")

            } catch (e: Exception) {
                Log.e("RiwayatVM-Sync", "Failed to sync LocalTX ID ${localTx.id}: $e", e)
            }
        }
        Log.i("RiwayatViewModel", "Sync: Sync process completed for unsynced transactions.")
    }

    private fun monitorNetworkStatus() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("RiwayatViewModel", "Network available, attempting sync.")
                viewModelScope.launch {
                    syncLocalTransactions()
                }
            }
            override fun onLost(network: Network) {
                Log.d("RiwayatViewModel", "Network lost.")
            }
        }
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: SecurityException) {
            Log.e("RiwayatViewModel", "Network callback permission issue.", e)
        }
        viewModelScope.coroutineContext.job.invokeOnCompletion {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            } catch (e: IllegalArgumentException) {
                Log.w("RiwayatViewModel", "Network callback already unregistered.")
            }
        }
    }

    suspend fun deleteTransaction(id: String): Result<Unit> {
        return try {
            if (id.startsWith("local_")) {
                val localId = id.removePrefix("local_").toLongOrNull()
                if (localId != null) {
                    val txToDelete = localTransactionDao.getTransactionByLocalId(localId)
                    txToDelete?.imageUri?.let { File(it).delete() }
                    localTransactionDao.deleteById(localId)
                    Log.d("RiwayatViewModel", "Deleted local-only transaction with local ID: $localId")
                } else {
                    Log.w("RiwayatViewModel", "Invalid local transaction ID format for deletion: $id")
                    return Result.failure(IllegalArgumentException("Invalid local transaction ID format"))
                }
            } else { // Ini berarti id adalah Firestore ID
                // Hapus dari Firestore
                db.collection("transactions")
                    .document(id)
                    .delete()
                    .await()
                Log.d("RiwayatViewModel", "Deleted Firestore transaction with ID: $id")

                // Hapus juga dari Room jika ada yang berkorespondensi (berdasarkan Firestore ID)
                val localTransaction = localTransactionDao.getByFirestoreId(id)
                if (localTransaction != null) {
                    localTransaction.imageUri?.let { File(it).delete() }
                    localTransactionDao.deleteById(localTransaction.id) // Hapus berdasarkan PrimaryKey Room (localTransaction.id)
                    Log.d("RiwayatViewModel", "Deleted corresponding local transaction with local ID: ${localTransaction.id} (Firestore ID: $id)")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RiwayatViewModel", "Error deleting transaction ID: $id, $e")
            Result.failure(e)
        }
    }
}