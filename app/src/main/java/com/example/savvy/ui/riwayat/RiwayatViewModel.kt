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
            }
    } ?: flowOf(emptyList<Transaction>())

    private val firestoreTransactionsFlow: Flow<List<Transaction>> = flow {
        val user = auth.currentUser
        if (user == null) {
            emit(emptyList<Transaction>())
            return@flow
        }
        try {
            val querySnapshot = db.collection("transactions")
                .whereEqualTo("userId", user.uid)
                .get()
                .await()
            val firestoreList = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(Transaction::class.java)?.copy(id = doc.id)
            }
            emit(firestoreList)
        } catch (e: Exception) {
            Log.e("RiwayatViewModel", "Firestore fetch error: $e")
            emit(emptyList<Transaction>())
        }
    }

    val transactions: StateFlow<List<Transaction>> = combine(
        localTransactionsFlow,
        firestoreTransactionsFlow
    ) { localTxsFromRoom, firestoreTxs ->
        Log.i("RiwayatViewModel", "Combining data: ${localTxsFromRoom.size} local, ${firestoreTxs.size} Firestore.")
        val transactionMap = mutableMapOf<String, Transaction>()

        firestoreTxs.forEach { firestoreTx ->
            if (firestoreTx.id.isNotBlank()) {
                transactionMap[firestoreTx.id] = firestoreTx
            }
        }

        localTxsFromRoom.forEach { mappedLocalTx ->
            val key = mappedLocalTx.id
            if (key.isBlank()) {
                return@forEach
            }

            val existingTransactionInMap = transactionMap[key]
            if (existingTransactionInMap != null) {
                transactionMap[key] = existingTransactionInMap.copy(
                    imageUri = mappedLocalTx.imageUri ?: existingTransactionInMap.imageUri,
                    imageUrl = existingTransactionInMap.imageUrl ?: mappedLocalTx.imageUrl,
                    type = mappedLocalTx.type,
                    amount = mappedLocalTx.amount,
                    category = mappedLocalTx.category,
                    note = mappedLocalTx.note,
                    date = mappedLocalTx.date,
                    walletId = mappedLocalTx.walletId
                )
            } else {
                transactionMap[key] = mappedLocalTx
            }
        }
        transactionMap.values.sortedByDescending { it.date }
    }.catch { e ->
        Log.e("RiwayatViewModel", "Combine operator error: $e")
        emit(emptyList<Transaction>())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList<Transaction>()
    )

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

    private suspend fun syncLocalTransactions() {
        val user = auth.currentUser ?: return
        val unsyncedTransactions = try {
            localTransactionDao.getUnsyncedTransactionsSync()
        } catch (e: Exception) {
            Log.e("RiwayatViewModel", "Sync: Error fetching unsynced: $e")
            emptyList()
        }

        if (unsyncedTransactions.isEmpty()) {
            Log.d("RiwayatViewModel", "Sync: No unsynced transactions.")
            return
        }
        Log.i("RiwayatViewModel", "Sync: Found ${unsyncedTransactions.size} unsynced transactions.")

        for (localTransaction in unsyncedTransactions) {
            if (localTransaction.firestoreId != null && localTransaction.isSynced) {
                continue
            }
            try {
                var firestoreCompatibleImageUrl: String? = localTransaction.imageUrl

                if (localTransaction.imageUri != null && firestoreCompatibleImageUrl.isNullOrBlank()) {
                    val imageFile = File(localTransaction.imageUri!!)
                    if (imageFile.exists()) {
                        val destinationFileName = "images/${System.currentTimeMillis()}_${imageFile.name}"
                        firestoreCompatibleImageUrl = withContext(Dispatchers.IO) {
                            uploader.uploadImage(imageFile, destinationFileName)
                        }
                    }
                }

                val firestoreTransaction = Transaction(
                    userId = localTransaction.userId,
                    walletId = localTransaction.walletId ?: "",
                    type = localTransaction.type,
                    amount = localTransaction.amount,
                    category = localTransaction.category,
                    note = localTransaction.note,
                    date = localTransaction.date,
                    imageUrl = firestoreCompatibleImageUrl,
                    imageUri = null
                )
                var finalFirestoreId = localTransaction.firestoreId

                if (finalFirestoreId.isNullOrBlank()) {
                    val documentReference = db.collection("transactions")
                        .add(firestoreTransaction)
                        .await()
                    finalFirestoreId = documentReference.id
                } else {
                    db.collection("transactions").document(finalFirestoreId)
                        .set(firestoreTransaction)
                        .await()
                }
                localTransactionDao.update(
                    localTransaction.copy(
                        isSynced = true,
                        firestoreId = finalFirestoreId,
                        imageUrl = firestoreCompatibleImageUrl
                    )
                )
            } catch (e: Exception) {
                Log.e("RiwayatViewModel", "Sync: Failed for local TX ID ${localTransaction.id}: $e", e)
            }
        }
        Log.i("RiwayatViewModel", "Sync: Process completed.")
    }

    suspend fun deleteTransaction(id: String): Result<Unit> {
        return try {
            if (id.startsWith("local_")) {
                val localId = id.removePrefix("local_").toLongOrNull()
                if (localId != null) {
                    val txToDelete = localTransactionDao.getTransactionByLocalId(localId)
                    txToDelete?.imageUri?.let { File(it).delete() }
                    localTransactionDao.deleteById(localId)
                } else {
                    return Result.failure(IllegalArgumentException("Invalid local ID format for deletion"))
                }
            } else {
                db.collection("transactions").document(id).delete().await()
                val localVersion = localTransactionDao.getByFirestoreId(id)
                if (localVersion != null) {
                    localVersion.imageUri?.let { File(it).delete() }
                    localTransactionDao.deleteById(localVersion.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RiwayatViewModel", "Error deleting transaction $id: $e")
            Result.failure(e)
        }
    }
}