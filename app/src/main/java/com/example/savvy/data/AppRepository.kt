package com.example.savvy.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val localTransactionDao: LocalTransactionDao,
    private val anggaranDao: AnggaranDao
) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    //    private val _wallets = MutableStateFlow<List<Wallet>>(emptyList())
    val wallets: Flow<List<Wallet>> = auth.currentUser?.uid?.let { userId ->
        db.collection("users").document(userId).collection("wallets")
            .orderBy("name")
            .snapshots()
            .mapNotNull { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Wallet::class.java)?.copy(id = doc.id, userId = userId)
                }
            }
    } ?: flowOf(emptyList())

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: Flow<List<Transaction>> = _transactions

    private val _firestoreAnggaran = MutableStateFlow<List<Anggaran>>(emptyList())
    private var anggaranListenerRegistration: ListenerRegistration? = null
    private var transactionsListenerRegistration: ListenerRegistration? = null // Tambahkan untuk transaksi

    private fun getLocalAnggaranFlow(userId: String): Flow<List<LocalAnggaran>> {
        return anggaranDao.getAllAnggaran(userId)
    }

    val anggaranList: Flow<List<LocalAnggaran>> = auth.currentUser?.uid?.let { userId ->
        combine(
            getLocalAnggaranFlow(userId),
            _firestoreAnggaran
        ) { localList, firestoreList ->
            val combinedList = mutableListOf<LocalAnggaran>()
            val firestoreMap = firestoreList.associateBy { it.id }

            localList.forEach { localAnggaran ->
                val firestoreAnggaran = firestoreMap[localAnggaran.firestoreId]
                if (firestoreAnggaran != null) {
                    val needsUpdate = localAnggaran.isSynced == false ||
                            localAnggaran.name != firestoreAnggaran.name ||
                            localAnggaran.category != firestoreAnggaran.category ||
                            localAnggaran.amount != firestoreAnggaran.amount ||
                            (localAnggaran.startDate != firestoreAnggaran.startDate && firestoreAnggaran.startDate != null) ||
                            (localAnggaran.endDate != firestoreAnggaran.endDate && firestoreAnggaran.endDate != null) ||
                            localAnggaran.clientGeneratedId != firestoreAnggaran.clientGeneratedId

                    if (needsUpdate) {
                        anggaranDao.update(localAnggaran.copy(
                            isSynced = true,
                            name = firestoreAnggaran.name,
                            category = firestoreAnggaran.category,
                            amount = firestoreAnggaran.amount,
                            startDate = firestoreAnggaran.startDate ?: localAnggaran.startDate,
                            endDate = firestoreAnggaran.endDate ?: localAnggaran.endDate,
                            clientGeneratedId = firestoreAnggaran.clientGeneratedId.ifBlank { localAnggaran.clientGeneratedId }
                        ))
                    }
                    combinedList.add(localAnggaran.copy(
                        isSynced = true,
                        clientGeneratedId = firestoreAnggaran.clientGeneratedId.ifBlank { localAnggaran.clientGeneratedId }
                    ))
                } else {
                    combinedList.add(localAnggaran)
                }
            }

            firestoreList.forEach { firestoreAnggaran ->
                val existingLocal = localList.firstOrNull { it.firestoreId == firestoreAnggaran.id }
                if (existingLocal == null) {
                    val existingLocalByClient = localList.firstOrNull { it.clientGeneratedId == firestoreAnggaran.clientGeneratedId }
                    if (existingLocalByClient != null) {
                        anggaranDao.update(existingLocalByClient.copy(
                            firestoreId = firestoreAnggaran.id,
                            isSynced = true,
                            name = firestoreAnggaran.name,
                            category = firestoreAnggaran.category,
                            amount = firestoreAnggaran.amount,
                            startDate = firestoreAnggaran.startDate ?: existingLocalByClient.startDate,
                            endDate = firestoreAnggaran.endDate ?: existingLocalByClient.endDate,
                            clientGeneratedId = firestoreAnggaran.clientGeneratedId.ifBlank { existingLocalByClient.clientGeneratedId }
                        ))
                        combinedList.add(existingLocalByClient.copy(
                            firestoreId = firestoreAnggaran.id,
                            isSynced = true,
                            clientGeneratedId = firestoreAnggaran.clientGeneratedId.ifBlank { existingLocalByClient.clientGeneratedId }
                        ))
                    } else {
                        val newLocalAnggaran = LocalAnggaran(
                            clientGeneratedId = firestoreAnggaran.clientGeneratedId.ifBlank { UUID.randomUUID().toString() },
                            userId = firestoreAnggaran.userId,
                            name = firestoreAnggaran.name,
                            category = firestoreAnggaran.category,
                            amount = firestoreAnggaran.amount,
                            startDate = firestoreAnggaran.startDate ?: Date(),
                            endDate = firestoreAnggaran.endDate ?: Date(),
                            isSynced = true,
                            firestoreId = firestoreAnggaran.id
                        )
                        anggaranDao.insert(newLocalAnggaran)
                        combinedList.add(newLocalAnggaran)
                    }
                }
            }
            combinedList.distinctBy { it.clientGeneratedId }
        }
    } ?: MutableStateFlow(emptyList())

    init {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            anggaranListenerRegistration = db.collection("anggarans")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("AppRepository", "Error fetching anggarans: $e")
                        _firestoreAnggaran.value = emptyList()
                        return@addSnapshotListener
                    }
                    val anggaranListResult = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Anggaran::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    _firestoreAnggaran.value = anggaranListResult
                    Log.d("AppRepository", "Real-time Anggaran update: ${anggaranListResult.size} items fetched.")
                }

            // Listener untuk Transactions (DITAMBAHKAN KEMBALI)
            transactionsListenerRegistration = db.collection("transactions")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("AppRepository", "Error fetching transactions: $e")
                        _transactions.value = emptyList()
                        return@addSnapshotListener
                    }
                    val transactionList = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Transaction::class.java)?.copy(id = doc.id, userId = userId)
                    } ?: emptyList()
                    _transactions.value = transactionList
                    Log.d("AppRepository", "Real-time Transaction update: ${transactionList.size} items fetched.")
                }
        }
    }

    // Fungsi untuk melepas listener jika diperlukan (misalnya saat logout)
    fun stopListeners() {
        anggaranListenerRegistration?.remove()
        transactionsListenerRegistration?.remove()
        anggaranListenerRegistration = null
        transactionsListenerRegistration = null
        Log.d("AppRepository", "All listeners stopped.")
    }

    fun stopAnggaranListener() {
        anggaranListenerRegistration?.remove()
        anggaranListenerRegistration = null
        Log.d("AppRepository", "Anggaran listener stopped.")
    }

    suspend fun createDefaultWalletsIfNotExist(userId: String) {
        val defaultWalletNames = listOf("Tunai", "Tabungan", "Non-Tunai")
        val walletsCollection = db.collection("users").document(userId).collection("wallets")

        val existingWalletsSnapshot = walletsCollection.get().await()
        if (existingWalletsSnapshot.isEmpty) {
            defaultWalletNames.forEach { name ->
                val wallet = Wallet(userId = userId, name = name, balance = 0L)
                try {
                    walletsCollection.add(wallet).await()
                    Log.d("AppRepository", "Default wallet '$name' created for user $userId")
                } catch (e: Exception) {
                    Log.e("AppRepository", "Error creating default wallet '$name': $e")
                }
            }
        } else {
            Log.d("AppRepository", "User $userId already has wallets, skipping default creation or checking for missing ones.")
            val existingWalletNames = existingWalletsSnapshot.documents.mapNotNull { it.getString("name") }
            defaultWalletNames.forEach { name ->
                if (!existingWalletNames.contains(name)) {
                    val wallet = Wallet(userId = userId, name = name, balance = 0L)
                    try {
                        walletsCollection.add(wallet).await()
                        Log.d("AppRepository", "Added missing default wallet '$name' for user $userId")
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Error adding missing default wallet '$name': $e")
                    }
                }
            }
        }
    }

    suspend fun addWallet(name: String, userId: String): Result<Unit> {
        return try {
            val walletsCollection = db.collection("users").document(userId).collection("wallets")
            val existingWallet = walletsCollection.whereEqualTo("name", name).limit(1).get().await()

            if (existingWallet.isEmpty) {
                val newWallet = Wallet(userId = userId, name = name, balance = 0L)
                walletsCollection.add(newWallet).await()
                Log.d("AppRepository", "Wallet '$name' added for user $userId")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Dompet dengan nama '$name' sudah ada."))
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error adding wallet: $e")
            Result.failure(e)
        }
    }

    suspend fun updateWalletName(walletId: String, newName: String, userId: String): Result<Unit> {
        return try {
            val walletsCollection = db.collection("users").document(userId).collection("wallets")
            val walletDocRef = walletsCollection.document(walletId)

            val otherWalletsWithNewName = walletsCollection.whereEqualTo("name", newName).get().await()
            if (otherWalletsWithNewName.documents.any { it.id != walletId }) {
                return Result.failure(Exception("Nama dompet '$newName' sudah digunakan oleh dompet lain."))
            }

            walletDocRef.update("name", newName).await()
            Log.d("AppRepository", "Wallet ID '$walletId' renamed to '$newName'")

            val batch = db.batch()
            val transactionsToUpdate = db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("walletId", walletId)
                .get().await()

            transactionsToUpdate.documents.forEach { doc ->
                Log.d("AppRepository", "Updating transaction ${doc.id} type to new wallet name $newName")
                batch.update(doc.reference, "type", newName)
            }
            batch.commit().await()
            Log.d("AppRepository", "Updated 'type' field for ${transactionsToUpdate.size()} transactions linked to wallet $walletId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AppRepository", "Error updating wallet name: $e", e)
            Result.failure(e)
        }
    }

    suspend fun hasTransactionsForWallet(walletId: String, userId: String): Boolean {
        return try {
            val querySnapshot = db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("walletId", walletId)
                .limit(1)
                .get()
                .await()
            !querySnapshot.isEmpty
        } catch (e: Exception) {
            Log.e("AppRepository", "Error checking transactions for wallet $walletId: $e")
            true
        }
    }

    suspend fun deleteWallet(walletId: String, userId: String): Result<Unit> {
        return try {
            val walletDocRef = db.collection("users").document(userId).collection("wallets").document(walletId)
            val walletDoc = walletDocRef.get().await()
            if (!walletDoc.exists()) {
                return Result.failure(Exception("Dompet tidak ditemukan."))
            }
            val walletName = walletDoc.getString("name")

            val defaultWallets = listOf("Tunai", "Tabungan", "Non-Tunai")
            if (defaultWallets.any { it.equals(walletName, ignoreCase = true) }) {
                return Result.failure(Exception("Dompet default ('$walletName') tidak bisa dihapus."))
            }

            if (hasTransactionsForWallet(walletId, userId)) {
                return Result.failure(Exception("Dompet '$walletName' tidak bisa dihapus karena masih memiliki transaksi terkait."))
            }

            walletDocRef.delete().await()
            Log.d("AppRepository", "Wallet ID '$walletId' ('$walletName') deleted for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AppRepository", "Error deleting wallet $walletId: $e")
            Result.failure(e)
        }
    }

    suspend fun insertAnggaran(localAnggaran: LocalAnggaran): String? {
        val userId = auth.currentUser?.uid ?: return null
        if (localAnggaran.userId != userId) {
            Log.e("AppRepo", "User ID mismatch for Anggaran")
            return null
        }

        val localId = anggaranDao.insert(localAnggaran.copy(isSynced = false, firestoreId = null))
        val tempLocalAnggaran = localAnggaran.copy(id = localId)

        try {
            val anggaranToSync = Anggaran(
                clientGeneratedId = tempLocalAnggaran.clientGeneratedId,
                userId = tempLocalAnggaran.userId,
                name = tempLocalAnggaran.name,
                category = tempLocalAnggaran.category,
                amount = tempLocalAnggaran.amount,
                startDate = tempLocalAnggaran.startDate,
                endDate = tempLocalAnggaran.endDate
            )
            val docRef = db.collection("anggarans").add(anggaranToSync).await()
            anggaranDao.update(tempLocalAnggaran.copy(firestoreId = docRef.id, isSynced = true))
            Log.i("AppRepo", "Anggaran synced to Firestore: ${docRef.id}")
            return docRef.id
        } catch (e: Exception) {
            Log.e("AppRepo", "Failed to sync anggaran to Firestore: $e")
        }
        return null
    }

    suspend fun updateAnggaran(localAnggaran: LocalAnggaran) {
        anggaranDao.update(localAnggaran.copy(isSynced = false))

        try {
            if (localAnggaran.firestoreId != null) {
                val anggaranToSync = Anggaran(
                    id = localAnggaran.firestoreId,
                    clientGeneratedId = localAnggaran.clientGeneratedId,
                    userId = localAnggaran.userId,
                    name = localAnggaran.name,
                    category = localAnggaran.category,
                    amount = localAnggaran.amount,
                    startDate = localAnggaran.startDate,
                    endDate = localAnggaran.endDate
                )
                db.collection("anggarans").document(localAnggaran.firestoreId).set(anggaranToSync).await()
                anggaranDao.update(localAnggaran.copy(isSynced = true))
                Log.i("AppRepo", "Anggaran updated in Firestore: ${localAnggaran.firestoreId}")
            } else {
                insertAnggaran(localAnggaran)
            }
        } catch (e: Exception) {
            Log.e("AppRepo", "Failed to sync updated anggaran to Firestore: $e")
        }
    }

    suspend fun deleteAnggaranByClientGeneratedId(clientGeneratedId: String) {
        val localAnggaran = anggaranDao.getAnggaranByClientGeneratedId(clientGeneratedId)
        anggaranDao.deleteByClientGeneratedId(clientGeneratedId)

        try {
            localAnggaran?.firestoreId?.let {
                db.collection("anggarans").document(it).delete().await()
                Log.i("AppRepo", "Anggaran deleted from Firestore: $it")
            }
        } catch (e: Exception) {
            Log.e("AppRepo", "Failed to delete anggaran from Firestore: $e")
        }
    }

    suspend fun onUserLogin() {
        val user = auth.currentUser
        if (user == null) {
            Log.w("AppRepository", "onUserLogin: User is null.")
            return
        }
        Log.d("AppRepository", "onUserLogin: Called for user ${user.uid}")

        createDefaultWalletsIfNotExist(user.uid)

        try {
            val firestoreTransactionsSnapshot = db.collection("transactions")
                .whereEqualTo("userId", user.uid)
                .get()
                .await()

            val firestoreTxList = firestoreTransactionsSnapshot.documents.mapNotNull { doc ->
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
            Log.i("AppRepository", "onUserLogin: Fetched ${firestoreTxList.size} TX from Firestore for user ${user.uid}")

            var newInsertsToRoom = 0
            var updatedInRoom = 0
            var alreadyInRoomAndMatched = 0

            for (txFromFirestore in firestoreTxList) {
                if (txFromFirestore.id.isBlank()) {
                    Log.w("AppRepository", "onUserLogin: Firestore TX has blank Firestore ID, skipping: $txFromFirestore")
                    continue
                }
                val existingLocal = localTransactionDao.getByFirestoreId(txFromFirestore.id)

                if (existingLocal != null) {
                    val needsUpdate = existingLocal.isSynced != true ||
                            existingLocal.imageUrl != txFromFirestore.imageUrl ||
                            existingLocal.type != txFromFirestore.type ||
                            existingLocal.amount != txFromFirestore.amount ||
                            existingLocal.category != txFromFirestore.category ||
                            existingLocal.note != txFromFirestore.note ||
                            (existingLocal.date != txFromFirestore.date && txFromFirestore.date != null) ||
                            existingLocal.walletId != txFromFirestore.walletId ||
                            existingLocal.clientGeneratedId != txFromFirestore.clientGeneratedId

                    if (needsUpdate) {
                        localTransactionDao.update(
                            existingLocal.copy(
                                clientGeneratedId = txFromFirestore.clientGeneratedId.ifBlank { existingLocal.clientGeneratedId },
                                userId = txFromFirestore.userId,
                                type = txFromFirestore.type,
                                amount = txFromFirestore.amount,
                                category = txFromFirestore.category,
                                note = txFromFirestore.note,
                                date = txFromFirestore.date ?: existingLocal.date,
                                imageUrl = txFromFirestore.imageUrl,
                                isSynced = true,
                                walletId = txFromFirestore.walletId
                            )
                        )
                        updatedInRoom++
                        Log.i("AppRepository", "onUserLogin: Updated existing Room TX (LocalID ${existingLocal.id}) with Firestore ID ${txFromFirestore.id}.")
                    } else {
                        alreadyInRoomAndMatched++
                        Log.d("AppRepository", "onUserLogin: Existing Room TX (LocalID ${existingLocal.id}) for Firestore ID ${txFromFirestore.id} is already up-to-date.")
                    }
                } else {
                    val localTxByClientUuid = if (txFromFirestore.clientGeneratedId.isNotBlank()) {
                        localTransactionDao.getByClientGeneratedId(txFromFirestore.clientGeneratedId)
                    } else null

                    if (localTxByClientUuid != null) {
                        Log.i("AppRepository", "onUserLogin: Found local TX by ClientUUID ${txFromFirestore.clientGeneratedId} (LocalID ${localTxByClientUuid.id}). Linking with FirestoreID ${txFromFirestore.id}.")
                        localTransactionDao.update(
                            localTxByClientUuid.copy(
                                firestoreId = txFromFirestore.id,
                                isSynced = true,
                                userId = txFromFirestore.userId,
                                type = txFromFirestore.type,
                                amount = txFromFirestore.amount,
                                category = txFromFirestore.category,
                                note = txFromFirestore.note,
                                date = txFromFirestore.date ?: localTxByClientUuid.date,
                                imageUrl = txFromFirestore.imageUrl,
                                walletId = txFromFirestore.walletId,
                                clientGeneratedId = txFromFirestore.clientGeneratedId.ifBlank { localTxByClientUuid.clientGeneratedId }
                            )
                        )
                        updatedInRoom++
                    } else {
                        Log.d("AppRepository", "onUserLogin: TX ${txFromFirestore.id} (ClientUUID: ${txFromFirestore.clientGeneratedId}) is new to Room. Inserting.")
                        val newLocalTransaction = LocalTransaction(
                            clientGeneratedId = txFromFirestore.clientGeneratedId.ifBlank { UUID.randomUUID().toString() },
                            userId = txFromFirestore.userId,
                            type = txFromFirestore.type,
                            amount = txFromFirestore.amount,
                            category = txFromFirestore.category,
                            note = txFromFirestore.note,
                            date = txFromFirestore.date ?: Date(),
                            imageUrl = txFromFirestore.imageUrl,
                            imageUri = null,
                            isSynced = true,
                            firestoreId = txFromFirestore.id,
                            walletId = txFromFirestore.walletId
                        )
                        localTransactionDao.insert(newLocalTransaction)
                        newInsertsToRoom++
                    }
                }
            }
            Log.i("AppRepository", "onUserLogin: Sync TX to Room complete. New inserts: $newInsertsToRoom, Updated in Room: $updatedInRoom, Already matched: $alreadyInRoomAndMatched")
        } catch (e: Exception) {
            Log.e("AppRepository", "onUserLogin: Error syncing Firestore TX to Room: $e", e)
        }

        try {
            val firestoreAnggaranSnapshot = db.collection("anggarans")
                .whereEqualTo("userId", user.uid)
                .get()
                .await()

            val firestoreAnggaranList = firestoreAnggaranSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Anggaran::class.java)?.copy(id = doc.id)
            }
            Log.i("AppRepository", "onUserLogin: Fetched ${firestoreAnggaranList.size} Anggaran from Firestore for user ${user.uid}")

            var newInsertsToRoomAnggaran = 0
            var updatedInRoomAnggaran = 0
            var alreadyInRoomAndMatchedAnggaran = 0

            firestoreAnggaranList.forEach { firestoreAnggaran ->
                val existingLocalAnggaran = anggaranDao.getAnggaranByFirestoreId(firestoreAnggaran.id)
                if (existingLocalAnggaran != null) {
                    val localNeedsUpdate = existingLocalAnggaran.isSynced != true ||
                            existingLocalAnggaran.name != firestoreAnggaran.name ||
                            existingLocalAnggaran.category != firestoreAnggaran.category ||
                            existingLocalAnggaran.amount != firestoreAnggaran.amount ||
                            (existingLocalAnggaran.startDate != firestoreAnggaran.startDate && firestoreAnggaran.startDate != null) ||
                            (existingLocalAnggaran.endDate != firestoreAnggaran.endDate && firestoreAnggaran.endDate != null) ||
                            existingLocalAnggaran.clientGeneratedId != firestoreAnggaran.clientGeneratedId

                    if (localNeedsUpdate) {
                        anggaranDao.update(
                            existingLocalAnggaran.copy(
                                name = firestoreAnggaran.name,
                                category = firestoreAnggaran.category,
                                amount = firestoreAnggaran.amount,
                                startDate = firestoreAnggaran.startDate ?: existingLocalAnggaran.startDate,
                                endDate = firestoreAnggaran.endDate ?: existingLocalAnggaran.endDate,
                                isSynced = true,
                                clientGeneratedId = firestoreAnggaran.clientGeneratedId.ifBlank { existingLocalAnggaran.clientGeneratedId }
                            )
                        )
                        updatedInRoomAnggaran++
                    } else {
                        alreadyInRoomAndMatchedAnggaran++
                    }
                } else {
                    val localAnggaranByClientUuid = if (firestoreAnggaran.clientGeneratedId.isNotBlank()) {
                        anggaranDao.getAnggaranByClientGeneratedId(firestoreAnggaran.clientGeneratedId)
                    } else null

                    if (localAnggaranByClientUuid != null) {
                        anggaranDao.update(
                            localAnggaranByClientUuid.copy(
                                firestoreId = firestoreAnggaran.id,
                                isSynced = true,
                                name = firestoreAnggaran.name,
                                category = firestoreAnggaran.category,
                                amount = firestoreAnggaran.amount,
                                startDate = firestoreAnggaran.startDate ?: localAnggaranByClientUuid.startDate,
                                endDate = firestoreAnggaran.endDate ?: localAnggaranByClientUuid.endDate,
                                clientGeneratedId = firestoreAnggaran.clientGeneratedId.ifBlank { localAnggaranByClientUuid.clientGeneratedId }
                            )
                        )
                        updatedInRoomAnggaran++
                    } else {
                        val newLocalAnggaran = LocalAnggaran(
                            clientGeneratedId = firestoreAnggaran.clientGeneratedId.ifBlank { UUID.randomUUID().toString() },
                            userId = firestoreAnggaran.userId,
                            name = firestoreAnggaran.name,
                            category = firestoreAnggaran.category,
                            amount = firestoreAnggaran.amount,
                            startDate = firestoreAnggaran.startDate ?: Date(),
                            endDate = firestoreAnggaran.endDate ?: Date(),
                            isSynced = true,
                            firestoreId = firestoreAnggaran.id
                        )
                        anggaranDao.insert(newLocalAnggaran)
                        newInsertsToRoomAnggaran++
                    }
                }
            }
            Log.i("AppRepository", "onUserLogin: Sync Anggaran to Room complete. New inserts: $newInsertsToRoomAnggaran, Updated in Room: $updatedInRoomAnggaran, Already matched: $alreadyInRoomAndMatchedAnggaran")
        } catch (e: Exception) {
            Log.e("AppRepository", "onUserLogin: Error syncing Firestore Anggaran to Room: $e", e)
        }
        syncUnsyncedAnggaran()
    }

    private suspend fun syncUnsyncedAnggaran() {
        val unsynced = anggaranDao.getUnsyncedAnggaran()
        if (unsynced.isEmpty()) return
        Log.i("AppRepo", "Found ${unsynced.size} unsynced Anggaran.")

        val user = auth.currentUser ?: run {
            Log.w("AppRepo-SyncAnggaran", "User null, cannot sync unsynced budgets.")
            return
        }

        for (local in unsynced) {
            try {
                var firestoreIdToUse = local.firestoreId
                val anggaranToSync = Anggaran(
                    clientGeneratedId = local.clientGeneratedId,
                    userId = local.userId,
                    name = local.name,
                    category = local.category,
                    amount = local.amount,
                    startDate = local.startDate,
                    endDate = local.endDate
                )

                if (firestoreIdToUse.isNullOrBlank()) {
                    val existingDocs = db.collection("anggarans")
                        .whereEqualTo("userId", user.uid)
                        .whereEqualTo("clientGeneratedId", local.clientGeneratedId)
                        .limit(1)
                        .get().await()

                    if (!existingDocs.isEmpty) {
                        firestoreIdToUse = existingDocs.documents[0].id
                        Log.i("AppRepo-SyncAnggaran", "Found existing Anggaran in Firestore by ClientUUID ${local.clientGeneratedId}, ID: $firestoreIdToUse. Updating Firestore and local.")
                        db.collection("anggarans").document(firestoreIdToUse).set(anggaranToSync).await()
                    } else {
                        val docRef = db.collection("anggarans").add(anggaranToSync).await()
                        firestoreIdToUse = docRef.id
                        Log.i("AppRepo-SyncAnggaran", "Synced new Anggaran to Firestore: $firestoreIdToUse")
                    }
                } else {
                    db.collection("anggarans").document(firestoreIdToUse).set(anggaranToSync).await()
                    Log.i("AppRepo-SyncAnggaran", "Synced updated Anggaran to Firestore: $firestoreIdToUse")
                }
                anggaranDao.update(local.copy(isSynced = true, firestoreId = firestoreIdToUse))
            } catch (e: Exception) {
                Log.e("AppRepo", "Failed to sync Anggaran (client ID: ${local.clientGeneratedId}): $e")
            }
        }
    }
}