//package com.example.savvy.ui.profile
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.savvy.data.AppRepository
//import com.example.savvy.data.Wallet
//import com.google.firebase.auth.FirebaseAuth
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
//@HiltViewModel
//class WalletViewModel @Inject constructor(
//    private val appRepository: AppRepository
//) : ViewModel() {
//
//    private val _wallets = MutableStateFlow<List<Wallet>>(emptyList())
//    val wallets: StateFlow<List<Wallet>> = _wallets.asStateFlow()
//
//    private val auth = FirebaseAuth.getInstance()
//
//    init {
//        collectWallets()
//        ensureDefaultWallets()
//    }
//
//    private fun collectWallets() {
//        viewModelScope.launch {
//            appRepository.wallets.collect { walletList ->
//                _wallets.value = walletList
//            }
//        }
//    }
//
//    private fun ensureDefaultWallets() {
//        viewModelScope.launch {
//            val userId = auth.currentUser?.uid ?: return@launch
//            val currentWallets = _wallets.value
//            val defaultWallets = listOf(
//                Wallet(id = "", userId = userId, name = "Tunai", balance = 0),
//                Wallet(id = "", userId = userId, name = "Tabungan", balance = 0)
//            )
//
//            defaultWallets.forEach { defaultWallet ->
//                if (currentWallets.none { it.name == defaultWallet.name }) {
//                    appRepository.insertWallet(defaultWallet)
//                }
//            }
//        }
//    }
//
//    fun addWallet(name: String) {
//        viewModelScope.launch {
//            val userId = auth.currentUser?.uid ?: return@launch
//            appRepository.insertWallet(Wallet(userId = userId, name = name, balance = 0))
//        }
//    }
//
//    fun updateWallet(wallet: Wallet) {
//        viewModelScope.launch {
//            val userId = auth.currentUser?.uid ?: return@launch
//            appRepository.updateWallet(wallet.copy(userId = userId))
//        }
//    }
//
//    fun deleteWallet(walletId: String) {
//        viewModelScope.launch {
//            appRepository.deleteWallet(walletId)
//        }
//    }
//}