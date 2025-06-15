package com.example.savvy.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val localTransactionDao: LocalTransactionDao,
    private val anggaranDao: AnggaranDao
) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // State Flow untuk menyimpan userId saat ini, menjadi pemicu utama
    private val _currentUserId = MutableStateFlow<String?>(auth.currentUser?.uid)

    // --- PERBAIKAN: Semua Flow data sekarang reaktif terhadap perubahan _currentUserId ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val wallets: Flow<List<Wallet>> = _currentUserId.flatMapLatest { userId ->
        if (userId == null) {
            flowOf(emptyList())
        } else {
            db.collection("users").document(userId).collection("wallets")
                .orderBy("name")
                .snapshots().map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Wallet::class.java)?.copy(id = doc.id, userId = userId)
                    }
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: Flow<List<Transaction>> = _currentUserId.flatMapLatest { userId ->
        if (userId == null) {
            flowOf(emptyList())
        } else {
            localTransactionDao.getAllTransactions(userId).map { localList ->
                localList.map { local ->
                    Transaction(
                        id = local.firestoreId ?: "local_${local.id}",
                        clientGeneratedId = local.clientGeneratedId,
                        userId = local.userId, type = local.type, amount = local.amount,
                        category = local.category, note = local.note, date = local.date,
                        imageUrl = local.imageUrl, imageUri = local.imageUri, walletId = local.walletId ?: ""
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val anggaranList: Flow<List<LocalAnggaran>> = _currentUserId.flatMapLatest { userId ->
        if (userId == null) {
            flowOf(emptyList())
        } else {
            anggaranDao.getAllAnggaran(userId)
        }
    }

    private var transactionsListener: ListenerRegistration? = null
    private var anggaranListener: ListenerRegistration? = null

    init {
        Log.d("AppRepository", "AppRepository instance created.")
        auth.currentUser?.uid?.let { startListeners(it) }
    }

    fun startListeners(userId: String) {
        if (transactionsListener != null) { stopListeners() }
        Log.i("AppRepository", "Starting Firestore listeners for user: $userId")

        transactionsListener = db.collection("transactions").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { Log.e("AppRepository", "Transactions listener error", e); return@addSnapshotListener }
                snapshot?.documents?.forEach { doc ->
                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)?.let { tx ->
                        CoroutineScope(Dispatchers.IO).launch { syncTransactionToLocal(tx) }
                    }
                }
            }

        anggaranListener = db.collection("anggarans").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { Log.e("AppRepository", "Anggaran listener error", e); return@addSnapshotListener }
                snapshot?.documents?.forEach { doc ->
                    doc.toObject(Anggaran::class.java)?.copy(id = doc.id)?.let { anggaran ->
                        CoroutineScope(Dispatchers.IO).launch { syncAnggaranToLocal(anggaran) }
                    }
                }
            }
    }

    fun stopListeners() {
        Log.i("AppRepository", "Stopping all Firestore listeners.")
        transactionsListener?.remove(); transactionsListener = null
        anggaranListener?.remove(); anggaranListener = null
    }

    fun updateUserSession(userId: String?) {
        Log.d("AppRepository", "Updating user session. New UserID: $userId")
        _currentUserId.value = userId
        if (userId != null) {
            startListeners(userId)
        } else {
            stopListeners()
        }
    }

    suspend fun onUserLogin() {
        val user = auth.currentUser ?: return
        createDefaultWalletsIfNotExist(user.uid)
        syncAllDataFromServer()
    }

    private suspend fun syncAllDataFromServer() {
        val user = auth.currentUser ?: return
        try {
            val firestoreTxList = db.collection("transactions").whereEqualTo("userId", user.uid).get().await().documents.mapNotNull { it.toObject<Transaction>()?.copy(id = it.id) }
            firestoreTxList.forEach { syncTransactionToLocal(it) }
            val firestoreAnggaranList = db.collection("anggarans").whereEqualTo("userId", user.uid).get().await().documents.mapNotNull { it.toObject<Anggaran>()?.copy(id = it.id) }
            firestoreAnggaranList.forEach { syncAnggaranToLocal(it) }
        } catch (e: Exception) { Log.e("AppRepository", "Error during full data sync: $e") }
    }

    private suspend fun syncTransactionToLocal(tx: Transaction) {
        val existingLocal = localTransactionDao.getByFirestoreId(tx.id) ?: if (tx.clientGeneratedId.isNotBlank()) localTransactionDao.getByClientGeneratedId(tx.clientGeneratedId) else null
        if (existingLocal == null) {
            localTransactionDao.insert(LocalTransaction(firestoreId = tx.id, clientGeneratedId = tx.clientGeneratedId, isSynced = true, userId = tx.userId, type = tx.type, amount = tx.amount, category = tx.category, note = tx.note, date = tx.date ?: Date(), imageUrl = tx.imageUrl, walletId = tx.walletId))
        } else { localTransactionDao.update(existingLocal.copy(firestoreId = tx.id, isSynced = true, type = tx.type, amount = tx.amount, category = tx.category, note = tx.note, date = tx.date ?: existingLocal.date, imageUrl = tx.imageUrl, walletId = tx.walletId)) }
    }

    private suspend fun syncAnggaranToLocal(anggaran: Anggaran) {
        val existingLocal = anggaranDao.getAnggaranByFirestoreId(anggaran.id) ?: if (anggaran.clientGeneratedId.isNotBlank()) anggaranDao.getAnggaranByClientGeneratedId(anggaran.clientGeneratedId) else null
        if (existingLocal == null) {
            anggaranDao.insert(LocalAnggaran(firestoreId = anggaran.id, clientGeneratedId = anggaran.clientGeneratedId.ifBlank { UUID.randomUUID().toString() }, isSynced = true, userId = anggaran.userId, name = anggaran.name, category = anggaran.category, amount = anggaran.amount, startDate = anggaran.startDate ?: Date(), endDate = anggaran.endDate ?: Date()))
        }
    }

    suspend fun deleteTransaction(id: String): Result<Unit> {
        return try {
            val localIdToDelete = if (id.startsWith("local_")) { id.removePrefix("local_").toLongOrNull() } else { localTransactionDao.getByFirestoreId(id)?.id }
            if (localIdToDelete != null) {
                val txToDelete = localTransactionDao.getTransactionByLocalId(localIdToDelete); txToDelete?.imageUri?.let { if(it.isNotBlank()) File(it).delete() }; localTransactionDao.deleteById(localIdToDelete)
            }
            if (!id.startsWith("local_")) { db.collection("transactions").document(id).delete().await() }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createDefaultWalletsIfNotExist(userId: String) {
        val defaultWalletNames = listOf("Tunai", "Tabungan", "Non-Tunai"); val walletsCollection = db.collection("users").document(userId).collection("wallets")
        try {
            val existingWalletsSnapshot = walletsCollection.get().await()
            if (existingWalletsSnapshot.isEmpty) { defaultWalletNames.forEach { name -> walletsCollection.add(Wallet(userId = userId, name = name, balance = 0L)).await() }
            }
        } catch (e: Exception) { Log.e("AppRepository", "Error checking/creating default wallets: $e") }
    }

    suspend fun addWallet(name: String, userId: String): Result<Unit> {
        return try {
            val walletsCollection = db.collection("users").document(userId).collection("wallets")
            val existingWallet = walletsCollection.whereEqualTo("name", name).limit(1).get().await()
            if (existingWallet.isEmpty) {
                walletsCollection.add(Wallet(userId = userId, name = name, balance = 0L)).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Dompet dengan nama '$name' sudah ada."))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateWalletName(walletId: String, newName: String, userId: String): Result<Unit> {
        return try {
            val walletsCollection = db.collection("users").document(userId).collection("wallets")
            val otherWallets = walletsCollection.whereEqualTo("name", newName).get().await()
            if (otherWallets.documents.any { it.id != walletId }) { return Result.failure(Exception("Nama dompet '$newName' sudah digunakan.")) }
            walletsCollection.document(walletId).update("name", newName).await()
            val batch = db.batch()
            val transactionsToUpdate = db.collection("transactions").whereEqualTo("userId", userId).whereEqualTo("walletId", walletId).get().await()
            transactionsToUpdate.documents.forEach { doc -> batch.update(doc.reference, "type", newName) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun hasTransactionsForWallet(walletId: String, userId: String): Boolean {
        return try { !db.collection("transactions").whereEqualTo("userId", userId).whereEqualTo("walletId", walletId).limit(1).get().await().isEmpty } catch (e: Exception) { true }
    }

    suspend fun deleteWallet(walletId: String, userId: String): Result<Unit> {
        return try {
            val walletDocRef = db.collection("users").document(userId).collection("wallets").document(walletId)
            val walletDoc = walletDocRef.get().await()
            if (!walletDoc.exists()) return Result.failure(Exception("Dompet tidak ditemukan."))
            val walletName = walletDoc.getString("name")
            if (listOf("Tunai", "Tabungan", "Non-Tunai").any { it.equals(walletName, ignoreCase = true) }) { return Result.failure(Exception("Dompet default tidak bisa dihapus.")) }
            if (hasTransactionsForWallet(walletId, userId)) { return Result.failure(Exception("Dompet '$walletName' tidak bisa dihapus karena masih memiliki transaksi.")) }
            walletDocRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun insertAnggaran(localAnggaran: LocalAnggaran) {
        val userId = auth.currentUser?.uid ?: return
        val localId = anggaranDao.insert(localAnggaran.copy(isSynced = false, firestoreId = null, userId = userId))
        val tempLocalAnggaran = localAnggaran.copy(id = localId, userId = userId)
        syncSpecificAnggaran(tempLocalAnggaran)
    }

    suspend fun updateAnggaran(localAnggaran: LocalAnggaran) {
        anggaranDao.update(localAnggaran.copy(isSynced = false))
        syncSpecificAnggaran(localAnggaran)
    }

    suspend fun deleteAnggaranByClientGeneratedId(clientGeneratedId: String) {
        try {
            val localAnggaran = anggaranDao.getAnggaranByClientGeneratedId(clientGeneratedId)
            anggaranDao.deleteByClientGeneratedId(clientGeneratedId)
            localAnggaran?.firestoreId?.let { db.collection("anggarans").document(it).delete().await() }
        } catch (e: Exception) { Log.e("AppRepo", "Failed to delete Anggaran from Firestore: $e") }
    }

    private suspend fun syncSpecificAnggaran(local: LocalAnggaran) {
        val user = auth.currentUser ?: return
        try {
            val anggaranToSync = Anggaran(clientGeneratedId = local.clientGeneratedId, userId = local.userId, name = local.name, category = local.category, amount = local.amount, startDate = local.startDate, endDate = local.endDate)
            var firestoreIdToUse = local.firestoreId
            if (firestoreIdToUse.isNullOrBlank()) {
                val existingDocs = db.collection("anggarans").whereEqualTo("userId", user.uid).whereEqualTo("clientGeneratedId", local.clientGeneratedId).limit(1).get().await()
                firestoreIdToUse = if (!existingDocs.isEmpty) { existingDocs.documents[0].id.also { db.collection("anggarans").document(it).set(anggaranToSync).await() } } else { db.collection("anggarans").add(anggaranToSync).await().id }
            } else {
                db.collection("anggarans").document(firestoreIdToUse).set(anggaranToSync).await()
            }
            anggaranDao.update(local.copy(isSynced = true, firestoreId = firestoreIdToUse))
        } catch (e: Exception) { Log.e("AppRepo", "Error during sync specific Anggaran: $e") }
    }
}
