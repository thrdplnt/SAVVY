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
            // Fetch local transactions
            val localTransactionsFlow = localTransactionDao.getAllTransactions(user.uid)
            val localTransactions = localTransactionsFlow.first().map { local ->
                Log.d("RiwayatViewModel", "Local transaction: $local, imageUri: ${local.imageUri}, firestoreId: ${local.firestoreId}")
                Transaction(
                    id = local.firestoreId ?: "local_${local.id}",
                    userId = local.userId,
                    type = local.type,
                    amount = local.amount,
                    category = local.category,
                    note = local.note,
                    date = local.date,
                    imageUrl = null, // Ignore imageUrl
                    imageUri = local.imageUri,
                    walletId = local.walletId ?: ""
                )
            }

            // Fetch Firestore transactions
            val firestoreTransactions = try {
                db.collection("transactions")
                    .whereEqualTo("userId", user.uid)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        doc.toObject(Transaction::class.java)?.copy(id = doc.id, imageUrl = null)
                    }
            } catch (e: Exception) {
                Log.e("RiwayatViewModel", "Failed to fetch Firestore transactions: $e")
                emptyList()
            }

            // Merge transactions using a map to avoid duplicates
            val transactionMap = mutableMapOf<String, Transaction>()
            for (localTx in localTransactions) {
                // Use firestoreId if available, otherwise use composite key
                val key = if (localTx.id.startsWith("local_")) {
                    "${localTx.userId}_${localTx.type}_${localTx.amount}_${localTx.category}_${localTx.note}_${localTx.date?.time}"
                } else {
                    localTx.id
                }
                transactionMap[key] = transactionMap[key]?.let { existing ->
                    existing.copy(
                        id = localTx.id, // Prioritize Firestore ID
                        imageUri = localTx.imageUri ?: existing.imageUri
                    )
                } ?: localTx
            }

            for (firestoreTx in firestoreTransactions) {
                val key = firestoreTx.id
                transactionMap[key] = transactionMap[key]?.let { existing ->
                    firestoreTx.copy(
                        id = firestoreTx.id,
                        imageUri = existing.imageUri // Preserve local imageUri
                    )
                } ?: firestoreTx
            }

            val combined = transactionMap.values
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
                // Check for duplicates based on key properties
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
                    // Transaction exists in Firestore, update Room
                    val firestoreId = existingFirestore.id
                    localTransactionDao.update(
                        localTransaction.copy(
                            isSynced = true,
                            firestoreId = firestoreId,
                            imageUrl = null // Ignore imageUrl
                        )
                    )
                    Log.d("RiwayatViewModel", "Found duplicate in Firestore, updated local transaction ID: ${localTransaction.id}, imageUri: ${localTransaction.imageUri}")
                    continue
                }

                // Save to Firestore without image
                val transaction = Transaction(
                    type = localTransaction.type,
                    amount = localTransaction.amount,
                    category = localTransaction.category,
                    note = localTransaction.note,
                    date = localTransaction.date,
                    userId = localTransaction.userId,
                    imageUrl = null,
                    imageUri = null, // Firestore doesn't store imageUri
                    walletId = localTransaction.walletId ?: ""
                )

                val documentReference = db.collection("transactions")
                    .add(transaction)
                    .await()
                Log.d("RiwayatViewModel", "Synced transaction to Firestore with ID: ${documentReference.id}")

                // Update Room, preserve imageUri
                localTransactionDao.update(
                    localTransaction.copy(
                        isSynced = true,
                        firestoreId = documentReference.id,
                        imageUrl = null
                    )
                )
                Log.d("RiwayatViewModel", "Marked local transaction ID: ${localTransaction.id} as synced, imageUri: ${localTransaction.imageUri}")
            } catch (e: Exception) {
                Log.e("RiwayatViewModel", "Failed to sync transaction ID ${localTransaction.id}: $e")
            }
        }
        Log.d("RiwayatViewModel", "Sync completed")
    }

    suspend fun deleteTransaction(id: String): Result<Unit> {
        return try {
            if (id.startsWith("local_")) {
                val localId = id.removePrefix("local_").toLongOrNull()
                if (localId != null) {
                    localTransactionDao.deleteById(localId)
                    Log.d("RiwayatViewModel", "Deleted local transaction with local ID: $localId")
                } else {
                    Log.w("RiwayatViewModel", "Invalid local transaction ID: $id")
                    return Result.failure(Exception("Invalid local transaction ID"))
                }
            } else {
                db.collection("transactions")
                    .document(id)
                    .delete()
                    .await()
                // Also delete from Room if it exists
                val localTransaction = localTransactionDao.getByFirestoreId(id)
                if (localTransaction != null) {
                    localTransactionDao.deleteById(localTransaction.id)
                    Log.d("RiwayatViewModel", "Deleted local transaction with firestoreId: $id")
                }
                Log.d("RiwayatViewModel", "Deleted Firestore transaction with ID: $id")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RiwayatViewModel", "Error deleting transaction ID: $id, $e")
            Result.failure(e)
        }
    }
}