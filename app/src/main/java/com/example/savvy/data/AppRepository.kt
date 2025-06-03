package com.example.savvy.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class AppRepository @Inject constructor(
    private val localTransactionDao: LocalTransactionDao,
    private val anggaranDao: AnggaranDao // <-- Tambahkan AnggaranDao
) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _wallets = MutableStateFlow<List<Wallet>>(emptyList())
    val wallets: Flow<List<Wallet>> = _wallets

    private val _firestoreAnggaran = MutableStateFlow<List<Anggaran>>(emptyList())
    private var anggaranListenerRegistration: ListenerRegistration? = null

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
                    combinedList.add(localAnggaran.copy(isSynced = true, clientGeneratedId = firestoreAnggaran.clientGeneratedId.ifBlank { localAnggaran.clientGeneratedId }))
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
                            firestoreId = firestoreAnggaran.id, isSynced = true, clientGeneratedId = firestoreAnggaran.clientGeneratedId.ifBlank { existingLocalByClient.clientGeneratedId }
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
            db.collection("users").document(userId).collection("wallets")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("AppRepository", "Error fetching wallets: $e")
                        _wallets.value = emptyList()
                        return@addSnapshotListener
                    }
                    val walletList = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Wallet::class.java)?.copy(id = doc.id, userId = userId)
                    } ?: emptyList()
                    _wallets.value = walletList
                }

            anggaranListenerRegistration = db.collection("anggarans")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("AppRepository", "Error fetching anggarans: $e")
                        _firestoreAnggaran.value = emptyList()
                        return@addSnapshotListener
                    }
                    val anggaranList = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Anggaran::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    _firestoreAnggaran.value = anggaranList
                    Log.d("AppRepository", "Real-time Anggaran update: ${anggaranList.size} items fetched.")
                }
        }
    }

    fun stopAnggaranListener() {
        anggaranListenerRegistration?.remove()
        anggaranListenerRegistration = null
        Log.d("AppRepository", "Anggaran listener stopped.")
    }

    suspend fun insertWallet(wallet: Wallet) { /* ... sama ... */ }

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
            Log.w("AppRepository", "onUserLogin: User is null. Cannot sync.")
            return
        }
        Log.d("AppRepository", "onUserLogin: Called for user: ${user.uid}")

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
                    clientGeneratedId = data["clientGeneratedId"] as? String ?: "", // PASTIKAN TIDAK NULL DI SINI
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
                var existingLocal = localTransactionDao.getByFirestoreId(txFromFirestore.id)
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
                    var localTxByClientUuid: LocalTransaction? = null
                    if (txFromFirestore.clientGeneratedId.isNotBlank()) {
                        localTxByClientUuid = localTransactionDao.getByClientGeneratedId(txFromFirestore.clientGeneratedId)
                    }

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

        // Sinkronisasi Anggaran (Tambahkan ini)
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
                var existingLocal = anggaranDao.getAnggaranByFirestoreId(firestoreAnggaran.id)
                if (existingLocal != null) {
                    val localNeedsUpdate = existingLocal.isSynced != true ||
                            existingLocal.name != firestoreAnggaran.name ||
                            existingLocal.category != firestoreAnggaran.category ||
                            existingLocal.amount != firestoreAnggaran.amount ||
                            (existingLocal.startDate != firestoreAnggaran.startDate && firestoreAnggaran.startDate != null) ||
                            (existingLocal.endDate != firestoreAnggaran.endDate && firestoreAnggaran.endDate != null) ||
                            existingLocal.clientGeneratedId != firestoreAnggaran.clientGeneratedId

                    if(localNeedsUpdate) {
                        anggaranDao.update(
                            existingLocal.copy(
                                name = firestoreAnggaran.name,
                                category = firestoreAnggaran.category,
                                amount = firestoreAnggaran.amount,
                                startDate = firestoreAnggaran.startDate ?: existingLocal.startDate,
                                endDate = firestoreAnggaran.endDate ?: existingLocal.endDate,
                                isSynced = true,
                                clientGeneratedId = firestoreAnggaran.clientGeneratedId.ifBlank { existingLocal.clientGeneratedId }
                            )
                        )
                        updatedInRoomAnggaran++
                    } else {
                        alreadyInRoomAndMatchedAnggaran++
                    }
                } else {
                    if (firestoreAnggaran.clientGeneratedId.isNotBlank()) {
                        existingLocal = anggaranDao.getAnggaranByClientGeneratedId(firestoreAnggaran.clientGeneratedId)
                    }
                    if (existingLocal != null) {
                        anggaranDao.update(
                            existingLocal.copy(
                                firestoreId = firestoreAnggaran.id,
                                isSynced = true,
                                name = firestoreAnggaran.name,
                                category = firestoreAnggaran.category,
                                amount = firestoreAnggaran.amount,
                                startDate = firestoreAnggaran.startDate ?: existingLocal.startDate,
                                endDate = firestoreAnggaran.endDate ?: existingLocal.endDate,
                                clientGeneratedId = firestoreAnggaran.clientGeneratedId.ifBlank { existingLocal.clientGeneratedId }
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