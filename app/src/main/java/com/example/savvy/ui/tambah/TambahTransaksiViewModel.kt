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
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import com.example.savvy.data.AppRepository // Import AppRepository
import com.example.savvy.data.Wallet // Import Wallet
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted // Import SharingStarted
import kotlinx.coroutines.flow.StateFlow // Import StateFlow
import kotlinx.coroutines.flow.stateIn // Import stateIn
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponse

@HiltViewModel
class TambahTransaksiViewModel @Inject constructor(
    private val uploader: SupabaseStorageUploader,
    private val localTransactionDao: LocalTransactionDao,
    private val appRepository: AppRepository, // <-- DITAMBAHKAN SEBAGAI PROPERTI
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance() // Tambahkan instance auth

    val wallets: StateFlow<List<Wallet>> = appRepository.wallets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Corrected line
            initialValue = emptyList()
        )

    init {
        monitorNetworkStatus()
    }

    fun getLocalTransactionByDbId(localId: Long, onResult: (LocalTransaction?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val localTx = localTransactionDao.getTransactionByLocalId(localId)
            withContext(Dispatchers.Main) {
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

                // Gunakan clientGeneratedId dari input jika ada (misal dari edit flow) atau generate baru
                val clientGeneratedIdToUse = if (transactionInput.clientGeneratedId.isNotBlank()) {
                    transactionInput.clientGeneratedId
                } else {
                    UUID.randomUUID().toString()
                }

                val localTransaction = LocalTransaction(
                    clientGeneratedId = clientGeneratedIdToUse, // PASTIKAN MENGGUNAKAN INI
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
                Log.d("TambahVM", "Inserted local TX (Room ID: $localDbId, ClientUUID: $clientGeneratedIdToUse)")

                if (withContext(Dispatchers.IO) { isNetworkAvailable(context) }) {
                    Log.d("TambahVM", "Online mode for ClientUUID: $clientGeneratedIdToUse")
                    var firestoreDocId: String? = null
                    var cloudImageUrl: String? = null

                    val existingDocs = db.collection("transactions")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("clientGeneratedId", clientGeneratedIdToUse)
                        .limit(1).get().await()

                    if (!existingDocs.isEmpty) {
                        val doc = existingDocs.documents[0]
                        firestoreDocId = doc.id
                        cloudImageUrl = doc.getString("imageUrl")
                        Log.i("TambahVM", "ClientUUID $clientGeneratedIdToUse already in Firestore (ID: $firestoreDocId).")

                        if (localImagePath != null && cloudImageUrl.isNullOrBlank()) {
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
                        // Perbarui dokumen yang sudah ada di Firestore
                        val firestoreTx = transactionInput.copy(
                            id = firestoreDocId, // Penting: sertakan ID Firestore
                            clientGeneratedId = clientGeneratedIdToUse,
                            imageUrl = cloudImageUrl,
                            imageUri = null
                        )
                        db.collection("transactions").document(firestoreDocId).set(firestoreTx).await()
                        Log.i("TambahVM", "Dokumen Firestore diperbarui (ID: $firestoreDocId) untuk ClientUUID: $clientGeneratedIdToUse")

                    } else {
                        Log.d("TambahVM", "ClientUUID $clientGeneratedIdToUse not in Firestore. Creating new doc.")
                        if (localImagePath != null) {
                            val file = File(localImagePath)
                            if (file.exists()) {
                                val destName = "images/${System.currentTimeMillis()}_${file.name}"
                                cloudImageUrl = withContext(Dispatchers.IO) { uploader.uploadImage(file, destName) }
                            }
                        }
                        val firestoreTx = transactionInput.copy(
                            clientGeneratedId = clientGeneratedIdToUse,
                            imageUrl = cloudImageUrl,
                            imageUri = null
                        )
                        val docRef = db.collection("transactions").add(firestoreTx).await()
                        firestoreDocId = docRef.id
                        Log.i("TambahVM", "Saved to Firestore (ID: $firestoreDocId) for ClientUUID: $clientGeneratedIdToUse")
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

                    if (firestoreDocId != null && transactionInput.category != "Pemasukan") {
                        triggerBudgetCheck(firestoreDocId)
                    }

                } else {
                    Log.d("TambahVM", "Offline mode. Saved to Room only (ClientUUID: $clientGeneratedIdToUse)")
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

                val transactionDate = transactionInput.date ?: Date()

                var currentLocalImagePath: String? = null
                var finalCloudImageUrl: String? = transactionInput.imageUrl

                if (isImageRemoved) {
                    finalCloudImageUrl = null
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
                        }
                    }

                    val firestoreTxData = transactionInput.copy(
                        clientGeneratedId = clientGeneratedIdToUse,
                        imageUrl = finalCloudImageUrl,
                        imageUri = null,
                        date = transactionDate,
                        id = ""
                    )

                    if (!finalFirestoreIdForUpdate.isNullOrBlank() && transactionInput.category != "Pemasukan") {
                        triggerBudgetCheck(finalFirestoreIdForUpdate)
                        db.collection("transactions").document(finalFirestoreIdForUpdate).set(firestoreTxData).await()
                        Log.i("TambahVM-Update", "Updated Firestore doc ID: $finalFirestoreIdForUpdate")
                    } else {
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
                            val newDocRef = db.collection("transactions").add(firestoreTxData).await()
                            finalFirestoreIdForUpdate = newDocRef.id
                            Log.i("TambahVM-Update", "No existing Firestore doc found by ID or ClientUUID. Created new: ${newDocRef.id}")
                        }
                    }
                }

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
                            imageUrl = finalCloudImageUrl,
                            isSynced = withContext(Dispatchers.IO) { isNetworkAvailable(context) },
                            firestoreId = finalFirestoreIdForUpdate ?: localTxToUpdate.firestoreId,
                            clientGeneratedId = clientGeneratedIdToUse
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

    private fun triggerBudgetCheck(transactionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userToken = auth.currentUser?.getIdToken(false)?.await()?.token
                if (userToken == null) {
                    Log.w("BudgetCheck", "User token is null, cannot trigger check.")
                    return@launch
                }

                Log.d("BudgetCheck", "Triggering budget check for tx: $transactionId")

                val url = "http://10.0.2.2:8000/check-budget"
                val body = """{ "transactionId": "$transactionId" }"""

                val (request, response, result) = Fuel.post(url)
                    .header("Authorization", "Bearer $userToken")
                    .header("Content-Type", "application/json")
                    .body(body)
                    .awaitStringResponse()

                Log.d("BudgetCheck", "Response from server [${response.statusCode}]: $result")
            } catch (e: Exception) {
                Log.e("BudgetCheck", "Gagal memanggil server untuk cek anggaran: $e")
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
            }
            override fun onLost(network: Network) {
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
        viewModelScope.launch {
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