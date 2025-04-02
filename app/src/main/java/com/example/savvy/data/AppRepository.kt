package com.example.savvy.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await

class AppRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // StateFlow untuk dompet
    private val _wallets = MutableStateFlow<List<Wallet>>(emptyList())
    val wallets: Flow<List<Wallet>> = _wallets

    // StateFlow untuk transaksi
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: Flow<List<Transaction>> = _transactions

    init {
        // Ambil data secara real-time
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = db.collection("users").document(userId)

            // Listener untuk wallets
            userRef.collection("wallets").addSnapshotListener { snapshot, _ ->
                val walletList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Wallet>()?.copy(id = doc.id, userId = userId)
                } ?: emptyList()
                _wallets.value = walletList
            }

            // Listener untuk transactions
            userRef.collection("transactions").addSnapshotListener { snapshot, _ ->
                val transactionList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Transaction>()?.copy(id = doc.id, userId = userId)
                } ?: emptyList()
                _transactions.value = transactionList
            }
        }
    }

    // Fungsi untuk menambahkan dompet
    suspend fun insertWallet(wallet: Wallet) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)
        userRef.collection("wallets").add(wallet).await()
    }

    // Fungsi untuk menambahkan transaksi
    suspend fun insertTransaction(transaction: Transaction) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        // Simpan transaksi
        userRef.collection("transactions").add(transaction).await()

        // Update saldo dompet
        val walletRef = userRef.collection("wallets").document(transaction.walletId)
        val wallet = walletRef.get().await().toObject<Wallet>()
        if (wallet != null) {
            val newBalance = if (transaction.type == "INCOME") {
                wallet.balance + transaction.amount
            } else {
                wallet.balance - transaction.amount
            }
            walletRef.update("balance", newBalance).await()
        }
    }
}