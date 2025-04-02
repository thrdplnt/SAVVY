package com.example.savvy.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvy.data.SupabaseStorageUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val uploader: SupabaseStorageUploader
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _profileState = MutableStateFlow(ProfileState())
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    fun updateProfile(name: String, imageUri: Uri?) {
        if (name.isBlank()) {
            _profileState.update { currentState ->
                currentState.copy(errorMessage = "Nama tidak boleh kosong")
            }
            return
        }

        _profileState.update { currentState ->
            currentState.copy(isLoading = true, errorMessage = null, isSuccess = false)
        }

        if (imageUri != null) {
            // Unggah gambar ke Supabase Storage
            viewModelScope.launch {
                try {
                    // Konversi URI ke File dengan kompresi
                    val inputStream = context.contentResolver.openInputStream(imageUri)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    // Kompresi gambar
                    val byteArrayOutputStream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
                    val compressedByteArray = byteArrayOutputStream.toByteArray()

                    // Simpan ke file sementara
                    val file = File(context.cacheDir, "temp_profile_image.jpg")
                    file.writeBytes(compressedByteArray)

                    // Unggah ke Supabase Storage
                    val destinationFileName = "profile_images/${System.currentTimeMillis()}_profile.jpg"
                    val imageUrl = uploader.uploadImage(file, destinationFileName)

                    if (imageUrl != null) {
                        // Perbarui profil dengan URL gambar baru
                        updateUserProfile(name, imageUrl)
                    } else {
                        _profileState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                errorMessage = "Gagal mengunggah foto profil",
                                isSuccess = false
                            )
                        }
                    }
                } catch (e: Exception) {
                    _profileState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorMessage = "Error: ${e.message}",
                            isSuccess = false
                        )
                    }
                }
            }
        } else {
            // Jika tidak ada gambar baru, langsung perbarui profil tanpa URL gambar baru
            updateUserProfile(name, auth.currentUser?.photoUrl?.toString())
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