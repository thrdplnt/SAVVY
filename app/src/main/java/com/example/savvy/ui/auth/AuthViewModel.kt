package com.example.savvy.ui.auth

import android.util.Log // Tambahkan Log jika belum ada
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvy.data.AppRepository // IMPORT AppRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel // IMPORT HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject // IMPORT Inject

// Data class AuthState (pastikan sudah ada dan sesuai, tambahkan successMessage jika belum)
data class AuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null // Untuk pesan seperti "email reset terkirim"
)

@HiltViewModel // Anotasi HiltViewModel
class AuthViewModel @Inject constructor( // Inject AppRepository
    private val appRepository: AppRepository
) : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun register(name: String, email: String, password: String, confirmPassword: String? = null) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.update { it.copy(isLoading = false, errorMessage = "Nama, email, dan password harus diisi", isSuccess = false) }
            return
        }
        if (confirmPassword != null && password != confirmPassword) {
            _authState.update { it.copy(isLoading = false, errorMessage = "Password dan konfirmasi password tidak cocok", isSuccess = false) }
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.update { it.copy(isLoading = false, errorMessage = "Email tidak valid", isSuccess = false) }
            return
        }
        if (password.length < 6) {
            _authState.update { it.copy(isLoading = false, errorMessage = "Password harus minimal 6 karakter", isSuccess = false) }
            return
        }

        _authState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            user.uid.let { userId ->
                                viewModelScope.launch {
                                    Log.d("AuthViewModel", "Registrasi berhasil, membuat dompet default untuk: $userId")

                                    appRepository.updateUserSession(userId) // Update sesi di repo
                                    appRepository.onUserLogin()
                                    appRepository.createDefaultWalletsIfNotExist(userId) // BUAT DOMPET DEFAULT

                                }
                            }
                            _authState.update { it.copy(isLoading = false, isSuccess = true) }
                        } else {
                            // Registrasi user berhasil, tapi update profil gagal
                            Log.w("AuthViewModel", "Registrasi user berhasil, tapi update profil gagal: ${profileTask.exception?.message}")
                            // Tetap anggap sukses registrasi, tapi mungkin beri pesan tambahan
                            user.uid.let { userId -> // Tetap buat dompet default
                                viewModelScope.launch {
                                    appRepository.onUserLogin()
                                    appRepository.createDefaultWalletsIfNotExist(userId)
                                }
                            }
                            _authState.update { it.copy(isLoading = false, isSuccess = true, successMessage = "Registrasi berhasil, namun update nama profil gagal.") }
                        }
                    } ?: run {
                        // Kasus user null setelah createUserWithEmailAndPassword (sangat jarang)
                        _authState.update { it.copy(isLoading = false, isSuccess = false, errorMessage = "Gagal mendapatkan info user setelah registrasi.") }
                    }
                } else {
                    _authState.update { it.copy(isLoading = false, isSuccess = false, errorMessage = task.exception?.message) }
                }
            }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            var errorMsg = "Email dan password harus diisi."
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() && email.isNotBlank()) errorMsg = "Email tidak valid."
            _authState.update { it.copy(isLoading = false, errorMessage = errorMsg, isSuccess = false) }
            return
        }
        _authState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.uid?.let { userId ->
                        viewModelScope.launch {
                            Log.d("AuthViewModel", "Login berhasil, memastikan dompet default ada untuk: $userId")
                            appRepository.updateUserSession(userId) // Update sesi di repo
                            appRepository.createDefaultWalletsIfNotExist(userId) // PASTIKAN DOMPET DEFAULT ADA
                            appRepository.onUserLogin() // Panggil juga onUserLogin dari AppRepository untuk sinkronisasi data lain
                        }
                    }
                    _authState.update { it.copy(isLoading = false, isSuccess = true) }
                } else {
                    _authState.update { it.copy(isLoading = false, isSuccess = false, errorMessage = task.exception?.message) }
                }
            }
    }

    fun resetPassword(email: String) {
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.update { it.copy(errorMessage = "Email tidak valid", successMessage = null) }
            return
        }
        _authState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                _authState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        successMessage = if (task.isSuccessful) "Email reset telah dikirim" else null,
                        errorMessage = if (!task.isSuccessful) task.exception?.message else null
                    )
                }
            }
    }

    fun logout() {
        auth.signOut()
        appRepository.updateUserSession(null) // Beri tahu repo bahwa tidak ada user
        _authState.update { AuthState() } // Reset ke state awal yang bersih
    }

    fun clearMessages() {
        _authState.update { currentState ->
            currentState.copy(errorMessage = null, successMessage = null)
        }
    }
}