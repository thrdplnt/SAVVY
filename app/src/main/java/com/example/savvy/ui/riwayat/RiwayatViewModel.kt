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
import com.example.savvy.data.LocalTransaction
import com.example.savvy.data.LocalTransactionDao
import com.example.savvy.data.SupabaseStorageUploader
import com.example.savvy.data.Transaction
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
import kotlinx.coroutines.sync.Mutex // IMPORT INI
import kotlinx.coroutines.sync.withLock // IMPORT INI
import java.io.File
import java.util.Date
import java.util.UUID
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
    private val syncMutex = Mutex() // DEKLARASIKAN MUTEX

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
                localTransactionList.map { local ->
                    Transaction(
                        id = local.firestoreId ?: "local_${local.id}",
                        clientGeneratedId = local.clientGeneratedId,
                        userId = local.userId,
                        type = local.type,
                        amount = local.amount,
                        category = local.category,
                        note = local.note,
                        date = local.date,
                        imageUrl = local.imageUrl,
                        imageUri = local.imageUri,
                        walletId = local.walletId ?: local.type
                    )
                }
            }
    } ?: flowOf(emptyList<Transaction>())

    private val firestoreTransactionsFlow: Flow<List<Transaction>> = flow {
        val user = auth.currentUser
        if (user == null) {
            emit(emptyList())
            return@flow
        }
        try {
            val querySnapshot = db.collection("transactions")
                .whereEqualTo("userId", user.uid)
                .get()
                .await()
            val firestoreList = querySnapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data == null) return@mapNotNull null
                Transaction(
                    id = doc.id,
                    clientGeneratedId = data["clientGeneratedId"] as? String ?: "",
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
        } catch (e: Exception) {
            Log.e("RiwayatViewModel", "Error fetching Firestore transactions: $e", e)
            emit(emptyList())
        }
    }

    val transactions: StateFlow<List<Transaction>> = combine(
        localTransactionsFlow,
        firestoreTransactionsFlow
    ) { localTxsFromRoom, firestoreTxs ->
        Log.i("RiwayatVM-Combine", "Combining: ${localTxsFromRoom.size} local, ${firestoreTxs.size} Firestore.")
        val combinedTransactions = mutableMapOf<String, Transaction>()

        firestoreTxs.forEach { firestoreTx ->
            val key = firestoreTx.clientGeneratedId.takeIf { it.isNotBlank() } ?: firestoreTx.id
            if (key.isNotBlank()) {
                combinedTransactions[key] = firestoreTx
            } else {
                Log.w("RiwayatVM-Combine", "Firestore TX memiliki kunci (clientGenId & id) kosong, dilewati: $firestoreTx")
            }
        }

        localTxsFromRoom.forEach { localTx ->
            val localKey = localTx.clientGeneratedId.takeIf { it.isNotBlank() } ?: localTx.id
            if (localKey.isBlank()) {
                Log.e("RiwayatVM-Combine", "Transaksi lokal memiliki clientGeneratedId atau id kosong, dilewati: $localTx")
                return@forEach
            }

            val existingTxInMap = combinedTransactions[localKey]

            if (existingTxInMap != null) {
                val updatedTx = existingTxInMap.copy(
                    id = existingTxInMap.id,
                    imageUri = localTx.imageUri ?: existingTxInMap.imageUri,
                    imageUrl = existingTxInMap.imageUrl ?: localTx.imageUrl
                )
                combinedTransactions[localKey] = updatedTx
            } else {
                combinedTransactions[localKey] = localTx
            }
        }

        combinedTransactions.values.map { tx ->
            if (tx.id.startsWith("local_")) {
                tx
            } else {
                tx.copy(id = tx.id)
            }
        }.distinctBy { it.id }.sortedByDescending { it.date }
    }.catch { e ->
        Log.e("RiwayatViewModel", "Combine operator error: $e", e)
        emit(emptyList<Transaction>())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList<Transaction>()
    )

    private suspend fun syncLocalTransactions() {
        // Gunakan Mutex untuk memastikan hanya satu sinkronisasi berjalan pada satu waktu
        syncMutex.withLock {
            val user = auth.currentUser ?: run {
                Log.w("RiwayatVM-Sync", "User null, cannot sync.")
                return
            }
            // Ambil lagi daftar transaksi yang belum disinkronkan setelah mengunci
            val unsyncedTransactions = try {
                localTransactionDao.getUnsyncedTransactionsSync()
            } catch (e: Exception) {
                Log.e("RiwayatVM-Sync", "Error fetching unsynced: $e", e)
                return
            }

            if (unsyncedTransactions.isEmpty()) {
                Log.d("RiwayatVM-Sync", "No unsynced transactions.")
                return
            }
            Log.i("RiwayatVM-Sync", "Found ${unsyncedTransactions.size} unsynced transactions to process.")

            for (localTx in unsyncedTransactions) {
                // Double check status synced di dalam loop, setelah fetching terbaru
                if (localTx.isSynced && !localTx.firestoreId.isNullOrBlank()) {
                    Log.d("RiwayatVM-Sync", "LocalTX ID ${localTx.id} (ClientUUID ${localTx.clientGeneratedId}) sudah ditandai sinkron dengan FirestoreID ${localTx.firestoreId}. Dilewati.")
                    continue
                }
                if (localTx.clientGeneratedId.isBlank()) {
                    Log.e("RiwayatVM-Sync", "LocalTX ID ${localTx.id} memiliki clientGeneratedId kosong, tidak dapat disinkronkan. Mungkin data rusak.")
                    continue
                }

                Log.d("RiwayatVM-Sync", "Processing LocalTX ID ${localTx.id}, ClientUUID ${localTx.clientGeneratedId}, FirestoreID: ${localTx.firestoreId}, Synced: ${localTx.isSynced}")

                try {
                    var finalFirestoreId = localTx.firestoreId
                    var cloudImageUrl = localTx.imageUrl

                    // 1. Cari dokumen di Firestore berdasarkan clientGeneratedId
                    val existingDocs = db.collection("transactions")
                        .whereEqualTo("userId", user.uid)
                        .whereEqualTo("clientGeneratedId", localTx.clientGeneratedId)
                        .limit(1)
                        .get()
                        .await()

                    if (!existingDocs.isEmpty) {
                        val doc = existingDocs.documents[0]
                        finalFirestoreId = doc.id
                        Log.i("RiwayatVM-Sync", "Ditemukan Firestore (ID: $finalFirestoreId) yang sudah ada untuk ClientUUID ${localTx.clientGeneratedId}.")

                        // Periksa apakah cloudImageUrl perlu diperbarui dari Firestore atau diunggah
                        if (cloudImageUrl.isNullOrBlank()) { // Jika lokal tidak punya cloudImageUrl
                            val firestoreCloudImageUrl = doc.getString("imageUrl")
                            if (!firestoreCloudImageUrl.isNullOrBlank()) {
                                cloudImageUrl = firestoreCloudImageUrl // Ambil dari Firestore
                            }
                        }

                        if (localTx.imageUri != null && (cloudImageUrl.isNullOrBlank() || cloudImageUrl == localTx.imageUrl)) { // Unggah jika URI lokal ada & cloud URL tidak valid/sama
                            val imageFile = File(localTx.imageUri)
                            if (imageFile.exists()) {
                                val destinationFileName = "images/${System.currentTimeMillis()}_${imageFile.name}"
                                Log.d("RiwayatVM-Sync", "Mengunggah gambar untuk LocalTX ID ${localTx.id} (Path: ${localTx.imageUri})")
                                val uploadedUrl = withContext(Dispatchers.IO) {
                                    uploader.uploadImage(imageFile, destinationFileName)
                                }
                                if (uploadedUrl != null) {
                                    cloudImageUrl = uploadedUrl
                                    Log.d("RiwayatVM-Sync", "Gambar diunggah untuk LocalTX ID ${localTx.id}. URL Cloud: $cloudImageUrl")
                                } else {
                                    Log.w("RiwayatVM-Sync", "Gagal mengunggah gambar untuk LocalTX ID ${localTx.id}.")
                                }
                            } else {
                                Log.w("RiwayatVM-Sync", "File gambar lokal tidak ditemukan untuk LocalTX ID ${localTx.id}: ${localTx.imageUri}")
                            }
                        }

                        // Perbarui dokumen yang sudah ada di Firestore
                        val transactionForFirestore = Transaction(
                            id = finalFirestoreId,
                            clientGeneratedId = localTx.clientGeneratedId,
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
                        db.collection("transactions").document(finalFirestoreId).set(transactionForFirestore).await()
                        Log.i("RiwayatVM-Sync", "Dokumen Firestore diperbarui (ID: $finalFirestoreId) untuk ClientUUID: ${localTx.clientGeneratedId}.")

                    } else {
                        // Dokumen TIDAK DITEMUKAN di Firestore berdasarkan clientGeneratedId, buat yang baru
                        Log.d("RiwayatVM-Sync", "Membuat dokumen Firestore baru untuk LocalTX ID ${localTx.id} (ClientUUID ${localTx.clientGeneratedId}).")
                        if (localTx.imageUri != null && cloudImageUrl.isNullOrBlank()) {
                            val imageFile = File(localTx.imageUri)
                            if (imageFile.exists()) {
                                val destinationFileName = "images/${System.currentTimeMillis()}_${imageFile.name}"
                                cloudImageUrl = withContext(Dispatchers.IO) { uploader.uploadImage(imageFile, destinationFileName) }
                            }
                        }
                        val transactionForFirestore = Transaction(
                            clientGeneratedId = localTx.clientGeneratedId,
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
                        val docRef = db.collection("transactions").add(transactionForFirestore).await()
                        finalFirestoreId = docRef.id
                        Log.i("RiwayatVM-Sync", "Dokumen Firestore baru dibuat (ID: $finalFirestoreId) untuk LocalTX ID ${localTx.id}.")
                    }

                    // 4. Update LocalTransaction di Room dengan status sinkron dan firestoreId yang benar
                    // PENTING: Lakukan ini di dalam blok try yang sama setelah operasi Firestore sukses
                    localTransactionDao.update(
                        localTx.copy(
                            isSynced = true,
                            firestoreId = finalFirestoreId,
                            imageUrl = cloudImageUrl
                        )
                    )
                    Log.d("RiwayatVM-Sync", "Menandai LocalTX ID ${localTx.id} sebagai sinkron. ID Firestore: $finalFirestoreId, Gambar Cloud: $cloudImageUrl")

                } catch (e: Exception) {
                    Log.e("RiwayatVM-Sync", "Gagal menyinkronkan LocalTX ID ${localTx.id}: $e", e)
                    // Jika terjadi kesalahan, JANGAN tandai sebagai synced. Biarkan isSynced = false.
                }
            }
            Log.i("RiwayatViewModel", "Sinkronisasi: Proses sinkronisasi selesai untuk transaksi yang belum disinkronkan.")
        } // end of withLock
    }

    private fun monitorNetworkStatus() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("RiwayatViewModel", "Jaringan tersedia, mencoba sinkronisasi.")
                viewModelScope.launch {
                    syncLocalTransactions()
                }
            }
            override fun onLost(network: Network) {
                Log.d("RiwayatViewModel", "Jaringan terputus.")
            }
        }
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: SecurityException) {
            Log.e("RiwayatViewModel", "Masalah izin panggilan balik jaringan.", e)
        }
        viewModelScope.coroutineContext.job.invokeOnCompletion {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            } catch (e: IllegalArgumentException) {
                Log.w("RiwayatViewModel", "Panggilan balik jaringan sudah tidak terdaftar.")
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
                    Log.d("RiwayatViewModel", "Menghapus transaksi hanya-lokal dengan ID lokal: $localId")
                } else {
                    Log.w("RiwayatViewModel", "Format ID transaksi lokal tidak valid untuk penghapusan: $id")
                    return Result.failure(IllegalArgumentException("Format ID transaksi lokal tidak valid"))
                }
            } else { // Ini berarti id adalah Firestore ID
                db.collection("transactions")
                    .document(id)
                    .delete()
                    .await()
                Log.d("RiwayatViewModel", "Menghapus transaksi Firestore dengan ID: $id")

                val localTransaction = localTransactionDao.getByFirestoreId(id)
                if (localTransaction != null) {
                    localTransaction.imageUri?.let { File(it).delete() }
                    localTransactionDao.deleteById(localTransaction.id)
                    Log.d("RiwayatViewModel", "Menghapus transaksi lokal yang sesuai dengan ID lokal: ${localTransaction.id} (Firestore ID: $id)")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RiwayatViewModel", "Gagal menghapus transaksi ID: $id, $e")
            Result.failure(e)
        }
    }
}