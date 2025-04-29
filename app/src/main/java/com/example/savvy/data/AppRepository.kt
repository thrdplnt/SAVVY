package com.example.savvy.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AppRepository @Inject constructor(
    private val localTransactionDao: LocalTransactionDao
) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _wallets = MutableStateFlow<List<Wallet>>(emptyList())
    val wallets: Flow<List<Wallet>> = _wallets

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: Flow<List<Transaction>> = _transactions

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
                    Log.d("AppRepository", "Updated wallets: ${walletList.size}")
                }

            db.collection("transactions")
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
                    Log.d("AppRepository", "Updated transactions: ${transactionList.size}")
                }
        }
    }

    suspend fun insertWallet(wallet: Wallet) {
        val userId = auth.currentUser?.uid ?: return
        try {
            db.collection("users").document(userId).collection("wallets")
                .add(wallet)
                .await()
            Log.d("AppRepository", "Inserted wallet: $wallet")
        } catch (e: Exception) {
            Log.e("AppRepository", "Error inserting wallet: $e")
            throw e
        }
    }

    suspend fun insertTransaction(transaction: Transaction) {
        val userId = auth.currentUser?.uid ?: return
        try {
            val documentRef = db.collection("transactions")
                .add(transaction.copy(userId = userId))
                .await()
            Log.d("AppRepository", "Inserted transaction to Firestore with ID: ${documentRef.id}")

            val existing = localTransactionDao.getByFirestoreId(documentRef.id)
            if (existing == null) {
                val localTransaction = LocalTransaction(
                    userId = userId,
                    type = transaction.type,
                    amount = transaction.amount,
                    category = transaction.category,
                    note = transaction.note,
                    date = transaction.date ?: java.util.Date(),
                    imageUrl = transaction.imageUrl,
                    isSynced = true,
                    firestoreId = documentRef.id
                )
                localTransactionDao.insert(localTransaction)
                Log.d("AppRepository", "Inserted transaction to Room: $localTransaction")
            } else {
                localTransactionDao.update(existing.copy(isSynced = true, firestoreId = documentRef.id))
                Log.d("AppRepository", "Updated existing transaction in Room: $existing")
            }

            val walletRef = db.collection("users").document(userId)
                .collection("wallets").document(transaction.walletId)
            val wallet = walletRef.get().await().toObject(Wallet::class.java)
            if (wallet != null) {
                val newBalance = if (transaction.category == "Pemasukan") {
                    wallet.balance + transaction.amount
                } else {
                    wallet.balance - transaction.amount
                }
                walletRef.update("balance", newBalance).await()
                Log.d("AppRepository", "Updated wallet balance: $newBalance")
            } else {
                Log.w("AppRepository", "Wallet not found for ID: ${transaction.walletId}")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error inserting transaction: $e")
            throw e
        }
    }

    suspend fun onUserLogin() {
        val user = auth.currentUser ?: return
        try {
            val firestoreTransactions = db.collection("transactions")
                .whereEqualTo("userId", user.uid)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                }

            for (transaction in firestoreTransactions) {
                val existing = localTransactionDao.getByFirestoreId(transaction.id)
                if (existing == null) {
                    val localTransaction = LocalTransaction(
                        userId = transaction.userId,
                        type = transaction.type,
                        amount = transaction.amount,
                        category = transaction.category,
                        note = transaction.note,
                        date = transaction.date ?: java.util.Date(),
                        imageUrl = transaction.imageUrl,
                        isSynced = true,
                        firestoreId = transaction.id
                    )
                    localTransactionDao.insert(localTransaction)
                    Log.d("AppRepository", "Synced Firestore transaction ${transaction.id} to Room")
                } else {
                    Log.d("AppRepository", "Transaction ${transaction.id} already exists in Room")
                }
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error syncing Firestore to Room: $e")
        }
    }
}
