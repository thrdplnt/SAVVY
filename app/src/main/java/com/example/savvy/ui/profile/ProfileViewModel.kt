package com.example.savvy.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvy.data.AppRepository
import com.example.savvy.data.LocalUser
import com.example.savvy.data.SupabaseStorageUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ProfileState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uploader: SupabaseStorageUploader,
    private val repository: AppRepository
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _profileState = MutableStateFlow(ProfileState())
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    val userProfile: Flow<LocalUser?> = repository.userProfile

    init {
        // Coba sinkronisasi saat ViewModel dibuat, jika ada yang belum sinkron
        viewModelScope.launch {
            repository.syncUserProfile(uploader)
        }
    }

    fun updateProfile(name: String, imageUri: Uri?, isPhotoRemoved: Boolean) {
        if (name.isBlank()) {
            _profileState.update { it.copy(errorMessage = "Nama tidak boleh kosong") }
            return
        }

        _profileState.update { it.copy(isLoading = true, errorMessage = null, isSuccess = false) }

        viewModelScope.launch {
            try {
                // 1. Panggil fungsi repository untuk menyimpan perubahan secara lokal
                repository.updateLocalProfile(name, imageUri, isPhotoRemoved, context)

                // 2. Coba sinkronkan langsung ke cloud
                repository.syncUserProfile(uploader)

                // 3. Set state sukses
                _profileState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _profileState.update { it.copy(isLoading = false, errorMessage = "Gagal memperbarui profil: ${e.message}") }
            }
        }
    }

    private fun updateUserProfile(name: String, photoUrl: String?) {
        val user = auth.currentUser ?: return

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .setPhotoUri(photoUrl?.let { Uri.parse(it) })
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _profileState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorMessage = null,
                            isSuccess = true
                        )
                    }
                } else {
                    _profileState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorMessage = "Gagal memperbarui profil: ${task.exception?.message}",
                            isSuccess = false
                        )
                    }
                }
            }
    }
}