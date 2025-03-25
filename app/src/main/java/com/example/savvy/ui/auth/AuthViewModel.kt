package com.example.savvy.ui.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun register(name: String, email: String, password: String, confirmPassword: String? = null) {
        // Validasi input
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            _authState.update { currentState -> currentState.copy(errorMessage = "Nama, email, dan password harus diisi") }
            return
        }

        // Validasi konfirmasi password
        if (confirmPassword != null && password != confirmPassword) {
            _authState.update { currentState -> currentState.copy(errorMessage = "Password dan konfirmasi password tidak cocok") }
            return
        }

        // Validasi format email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.update { currentState -> currentState.copy(errorMessage = "Email tidak valid") }
            return
        }

        // Validasi panjang password (minimal 6 karakter, sesuai kebijakan Firebase)
        if (password.length < 6) {
            _authState.update { currentState -> currentState.copy(errorMessage = "Password harus minimal 6 karakter") }
            return
        }

        _authState.update { currentState -> currentState.copy(isLoading = true) }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Set displayName setelah registrasi berhasil
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            _authState.update { currentState ->
                                currentState.copy(
                                    isLoading = false,
                                    isSuccess = true,
                                    errorMessage = null
                                )
                            }
                        } else {
                            _authState.update { currentState ->
                                currentState.copy(
                                    isLoading = false,
                                    isSuccess = false,
                                    errorMessage = profileTask.exception?.message
                                )
                            }
                        }
                    }
                } else {
                    _authState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            isSuccess = false,
                            errorMessage = task.exception?.message
                        )
                    }
                }
            }
    }

    fun login(email: String, password: String) {
        // Validasi input
        if (email.isEmpty() || password.isEmpty()) {
            _authState.update { currentState -> currentState.copy(errorMessage = "Email dan password harus diisi") }
            return
        }

        // Validasi format email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.update { currentState -> currentState.copy(errorMessage = "Email tidak valid") }
            return
        }

        _authState.update { currentState -> currentState.copy(isLoading = true) }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _authState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        isSuccess = task.isSuccessful,
                        errorMessage = if (!task.isSuccessful) task.exception?.message else null
                    )
                }
            }
    }

    fun resetPassword(email: String) {
        // Validasi input
        if (email.isEmpty()) {
            _authState.update { currentState -> currentState.copy(errorMessage = "Email harus diisi") }
            return
        }

        // Validasi format email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.update { currentState -> currentState.copy(errorMessage = "Email tidak valid") }
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                _authState.update { currentState ->
                    currentState.copy(
                        errorMessage = if (task.isSuccessful) "Email reset telah dikirim"
                        else task.exception?.message
                    )
                }
            }
    }

    fun logout() {
        auth.signOut()
        _authState.update { currentState -> currentState.copy(isSuccess = false, isLoading = false, errorMessage = null) }
    }
}

data class AuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)