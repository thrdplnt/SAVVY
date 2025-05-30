package com.example.savvy.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject // Import ini jika belum ada
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class AppRepository @Inject constructor(
    private val localTransactionDao: LocalTransactionDao
) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _wallets = MutableStateFlow<List<Wallet>>(emptyList())
    val wallets: Flow<List<Wallet>> = _wallets

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
        }
    }

    suspend fun insertWallet(wallet: Wallet) {
        // ... (implementasi Anda sudah oke)
    }

    // insertTransaction di AppRepository mungkin tidak lagi jadi jalur utama jika
    // TambahTransaksiViewModel langsung berinteraksi dengan DAO dan Firestore.
    // Jika masih dipakai, pastikan ia juga menggunakan clientGeneratedId dengan benar.

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
                // Mapping manual untuk lebih kontrol atas field nullable seperti clientGeneratedId
                val data = doc.data
                if (data == null) return@mapNotNull null
                Transaction(
                    id = doc.id, // Firestore Document ID
                    clientGeneratedId = data["clientGeneratedId"] as? String, // Bisa null dari Firestore
                    userId = data["userId"] as? String ?: user.uid, // Fallback ke user.uid jika missing
                    walletId = data["walletId"] as? String ?: (data["type"] as? String ?: ""), // Fallback type lama ke walletId
                    type = data["type"] as? String ?: "", // Ini adalah tipe sumber dana / nama dompet
                    amount = data["amount"] as? Long ?: 0L,
                    category = data["category"] as? String ?: "",
                    note = data["note"] as? String ?: "",
                    date = (data["date"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                    imageUrl = data["imageUrl"] as? String,
                    imageUri = null // Tidak relevan dari Firestore
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

                // 1. Coba cari di Room berdasarkan Firestore ID (ini kunci utama sinkronisasi)
                var existingLocal = localTransactionDao.getByFirestoreId(txFromFirestore.id)

                if (existingLocal != null) {
                    // Ditemukan berdasarkan Firestore ID. Ini adalah jalur update utama.
                    Log.d("AppRepository", "onUserLogin: Found local TX by FirestoreID ${txFromFirestore.id} (LocalID: ${existingLocal.id}). Comparing for update.")
                    // Cek apakah perlu diupdate
                    val needsUpdate = existingLocal.isSynced != true || // Jika belum ditandai synced
                            existingLocal.imageUrl != txFromFirestore.imageUrl ||
                            existingLocal.type != txFromFirestore.type ||
                            existingLocal.amount != txFromFirestore.amount ||
                            existingLocal.category != txFromFirestore.category ||
                            existingLocal.note != txFromFirestore.note ||
                            existingLocal.date != (txFromFirestore.date ?: existingLocal.date) ||
                            existingLocal.walletId != txFromFirestore.walletId ||
                            (existingLocal.clientGeneratedId != txFromFirestore.clientGeneratedId && !txFromFirestore.clientGeneratedId.isNullOrBlank())

                    if (needsUpdate) {
                        localTransactionDao.update(
                            existingLocal.copy(
                                clientGeneratedId = txFromFirestore.clientGeneratedId ?: existingLocal.clientGeneratedId, // Update jika ada dari Firestore
                                userId = txFromFirestore.userId,
                                type = txFromFirestore.type,
                                amount = txFromFirestore.amount,
                                category = txFromFirestore.category,
                                note = txFromFirestore.note,
                                date = txFromFirestore.date ?: existingLocal.date,
                                imageUrl = txFromFirestore.imageUrl,
                                isSynced = true, // Tandai sudah sinkron
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
                    // Tidak ditemukan berdasarkan Firestore ID.
                    // Ini berarti:
                    // a) Transaksi ini benar-benar baru untuk device ini.
                    // b) Transaksi ini mungkin sudah ada di Room (dibuat offline) tapi belum terhubung (firestoreId-nya null).
                    //    Kita coba cari berdasarkan clientGeneratedId jika ada.
                    var localTxByClientUuid: LocalTransaction? = null
                    if (!txFromFirestore.clientGeneratedId.isNullOrBlank()) {
                        localTxByClientUuid = localTransactionDao.getByClientGeneratedId(txFromFirestore.clientGeneratedId!!)
                    }

                    if (localTxByClientUuid != null) {
                        // Ditemukan di Room berdasarkan clientGeneratedId! Ini transaksi yang dibuat offline dan sekarang ketemu pasangannya dari server.
                        // Update entri lokal ini dengan firestoreId dan data dari server.
                        Log.i("AppRepository", "onUserLogin: Found local TX by ClientUUID ${txFromFirestore.clientGeneratedId} (LocalID ${localTxByClientUuid.id}). Linking with FirestoreID ${txFromFirestore.id}.")
                        localTransactionDao.update(
                            localTxByClientUuid.copy(
                                firestoreId = txFromFirestore.id, // INI YANG PENTING: Menghubungkan!
                                isSynced = true,
                                // Update field lain dari server untuk konsistensi
                                userId = txFromFirestore.userId,
                                type = txFromFirestore.type,
                                amount = txFromFirestore.amount,
                                category = txFromFirestore.category,
                                note = txFromFirestore.note,
                                date = txFromFirestore.date ?: localTxByClientUuid.date,
                                imageUrl = txFromFirestore.imageUrl, // imageUri lokal dipertahankan
                                walletId = txFromFirestore.walletId
                            )
                        )
                        updatedInRoom++
                    } else {
                        // Benar-benar baru untuk Room. Insert.
                        Log.d("AppRepository", "onUserLogin: TX ${txFromFirestore.id} (ClientUUID: ${txFromFirestore.clientGeneratedId}) is new to Room. Inserting.")
                        val newLocalTransaction = LocalTransaction(
                            clientGeneratedId = txFromFirestore.clientGeneratedId ?: UUID.randomUUID().toString(), // Buat baru jika dari Firestore null/kosong (data lama)
                            userId = txFromFirestore.userId,
                            type = txFromFirestore.type,
                            amount = txFromFirestore.amount,
                            category = txFromFirestore.category,
                            note = txFromFirestore.note,
                            date = txFromFirestore.date ?: Date(),
                            imageUrl = txFromFirestore.imageUrl,
                            imageUri = null, // Dari server, tidak ada imageUri lokal awal
                            isSynced = true,
                            firestoreId = txFromFirestore.id,
                            walletId = txFromFirestore.walletId
                        )
                        localTransactionDao.insert(newLocalTransaction)
                        newInsertsToRoom++
                    }
                }
            }
            Log.i("AppRepository", "onUserLogin: Sync to Room complete. New inserts: $newInsertsToRoom, Updated in Room: $updatedInRoom, Already matched: $alreadyInRoomAndMatched")

        } catch (e: Exception) {
            Log.e("AppRepository", "onUserLogin: Error syncing Firestore to Room: $e", e)
        }
    }
}