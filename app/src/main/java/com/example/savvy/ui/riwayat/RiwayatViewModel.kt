package com.example.savvy.ui.riwayat

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvy.data.*
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
            appRepository.onUserLogin()
        }
    }

    val transactions: StateFlow<List<Transaction>> = flow {
        Log.d("RiwayatViewModel", "Fetching transactions")
        val user = auth.currentUser ?: run {
            Log.w("RiwayatViewModel", "No authenticated user")
            emit(emptyList())
            return@flow
        }
        try {
            // Ambil transaksi lokal
            val localTransactionsFlow = localTransactionDao.getAllTransactions(user.uid)
            val localTransactions = localTransactionsFlow.first().map { local ->
                Log.d("RiwayatViewModel", "Local transaction: $local, imageUri: ${local.imageUri}")
                Transaction(
                    id = local.firestoreId ?: "local_${local.id}",
                    userId = local.userId,
                    type = local.type,
                    amount = local.amount,
                    category = local.category,
                    note = local.note,
                    date = local.date,
                    imageUrl = local.imageUrl,
                    imageUri = local.imageUri,
                    walletId = local.walletId ?: ""
                )
            }

            // Ambil transaksi Firestore
            val firestoreTransactions = try {
                db.collection("transactions")
                    .whereEqualTo("userId", user.uid)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                    }
            } catch (e: Exception) {
                Log.e("RiwayatViewModel", "Failed to fetch Firestore transactions: $e")
                emptyList()
            }

            // Gabungkan dan hapus duplikasi
            val combined = (localTransactions + firestoreTransactions)
                .distinctBy { it.id }
                .sortedByDescending { it.date }
            Log.d("RiwayatViewModel", "Emitting ${combined.size} transactions")
            emit(combined)
        } catch (e: Exception) {
            Log.e("RiwayatViewModel", "Error fetching transactions: $e")
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun monitorNetworkStatus() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("RiwayatViewModel", "Network available, attempting to sync local transactions")
                viewModelScope.launch {
                    syncLocalTransactions()
                }
            }

            override fun onLost(network: Network) {
                Log.d("RiwayatViewModel", "Network lost")
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        viewModelScope.launch {
            viewModelScope.coroutineContext.job.invokeOnCompletion {
                connectivityManager.unregisterNetworkCallback(networkCallback)
                Log.d("RiwayatViewModel", "Unregistered network callback")
            }
        }
    }

    private suspend fun syncLocalTransactions() {
        val user = auth.currentUser ?: return
        val unsyncedTransactions = try {
            localTransactionDao.getUnsyncedTransactionsSync()
        } catch (e: Exception) {
            Log.e("RiwayatViewModel", "Error fetching unsynced transactions: $e")
            emptyList()
        }

        if (unsyncedTransactions.isEmpty()) {
            Log.d("RiwayatViewModel", "No unsynced transactions to sync")
            return
        }

        Log.d("RiwayatViewModel", "Starting sync for ${unsyncedTransactions.size} unsynced transactions")
        for (localTransaction in unsyncedTransactions) {
            try {
                // Cek duplikasi berdasarkan firestoreId
                if (localTransaction.firestoreId != null) {
                    val existing = localTransactionDao.getByFirestoreId(localTransaction.firestoreId)
                    if (existing != null) {
                        Log.d("RiwayatViewModel", "Transaction with firestoreId ${localTransaction.firestoreId} already exists, skipping")
                        continue
                    }
                }

                // Cek duplikasi berdasarkan properti
                val existingFirestore = db.collection("transactions")
                    .whereEqualTo("userId", localTransaction.userId)
                    .whereEqualTo("type", localTransaction.type)
                    .whereEqualTo("amount", localTransaction.amount)
                    .whereEqualTo("category", localTransaction.category)
                    .whereEqualTo("note", localTransaction.note)
                    .whereEqualTo("date", localTransaction.date)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()

                if (existingFirestore != null) {
                    val firestoreId = existingFirestore.id
                    val imageUrl = existingFirestore.toObject(Transaction::class.java)?.imageUrl
                    localTransactionDao.update(
                        localTransaction.copy(
                            isSynced = true,
                            firestoreId = firestoreId,
                            imageUrl = imageUrl
                        )
                    )
                    Log.d("RiwayatViewModel", "Found duplicate in Firestore, updated local transaction ID: ${localTransaction.id}, imageUri: ${localTransaction.imageUri}")
                    continue
                }

                // Upload gambar jika ada
                var imageUrl: String? = null
                if (localTransaction.imageUri != null) {
                    val file = File(localTransaction.imageUri)
                    if (file.exists()) {
                        val destinationFileName = "images/${System.currentTimeMillis()}_image.jpg"
                        imageUrl = withContext(Dispatchers.IO) {
                            uploader.uploadImage(file, destinationFileName)
                        }
                        Log.d("RiwayatViewModel", "Uploaded image: $imageUrl, local imageUri: ${localTransaction.imageUri}")
                    } else {
                        Log.w("RiwayatViewModel", "Image file not found: ${localTransaction.imageUri}")
                    }
                }

                // Simpan ke Firestore
                val transaction = Transaction(
                    type = localTransaction.type,
                    amount = localTransaction.amount,
                    category = localTransaction.category,
                    note = localTransaction.note,
                    date = localTransaction.date,
                    userId = localTransaction.userId,
                    imageUrl = imageUrl,
                    walletId = localTransaction.walletId ?: ""
                )

                val documentReference = db.collection("transactions")
                    .add(transaction)
                    .await()
                Log.d("RiwayatViewModel", "Synced transaction to Firestore with ID: ${documentReference.id}")

                // Perbarui Room, pertahankan imageUri
                localTransactionDao.update(
                    localTransaction.copy(
                        isSynced = true,
                        firestoreId = documentReference.id,
                        imageUrl = imageUrl
                    )
                )
                Log.d("RiwayatViewModel", "Marked local transaction ID: ${localTransaction.id} as synced, imageUri: ${localTransaction.imageUri}")
            } catch (e: Exception) {
                Log.e("RiwayatViewModel", "Failed to sync transaction ID ${localTransaction.id}: $e")
            }
        }
        Log.d("RiwayatViewModel", "Sync completed")
    }
}