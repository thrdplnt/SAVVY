//package com.example.savvy.ui.riwayat
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.savvy.data.Transaction
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.FirebaseFirestoreException
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//import javax.inject.Inject
//
//@HiltViewModel
//class RiwayatViewModel @Inject constructor() : ViewModel() {
//
//    private val db = FirebaseFirestore.getInstance()
//    private val auth = FirebaseAuth.getInstance()
//
//    // State for transactions
//    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
//    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()
//
//    // State for loading
//    private val _isLoading = MutableStateFlow(true)
//    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
//
//    // State for error messages
//    private val _errorMessage = MutableStateFlow<String?>(null)
//    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
//
//    init {
//        fetchTransactions()
//    }
//
//    fun fetchTransactions() {
//        val userId = auth.currentUser?.uid ?: return
//        viewModelScope.launch {
//            try {
//                _isLoading.value = true
//                val snapshot = db.collection("users")
//                    .document(userId)
//                    .collection("transactions")
//                    .get()
//                    .await()
//
//                val transactionList = snapshot.documents.mapNotNull { doc ->
//                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)
//                }
//                _transactions.value = transactionList
//            } catch (e: FirebaseFirestoreException) {
//                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
//                    _errorMessage.value = "Izin ditolak: Anda tidak memiliki akses untuk melihat transaksi."
//                } else {
//                    _errorMessage.value = "Gagal mengambil data transaksi: ${e.message}"
//                }
//            } catch (e: Exception) {
//                _errorMessage.value = "Gagal mengambil data transaksi: ${e.message}"
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
//
//    fun clearErrorMessage() {
//        _errorMessage.value = null
//    }
//}