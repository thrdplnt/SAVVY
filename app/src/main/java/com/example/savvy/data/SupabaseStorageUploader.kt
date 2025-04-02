package com.example.savvy.data

import android.content.Context
import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseStorageUploader @Inject constructor(
    private val context: Context
) {
    private val supabase = createSupabaseClient(
        supabaseUrl = "https://kekhqoqhuehrmkpwgtun.supabase.co", // Ganti dengan Project URL Anda
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imtla2hxb3FodWVocm1rcHdndHVuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDM1MzA5NzEsImV4cCI6MjA1OTEwNjk3MX0.goZF5vffYxoyUDMAc6zkzTKeM4_CBEFkGOfmYIDxWLs" // Ganti dengan Anon Key Anda
    ) {
        install(Storage)
    }

        suspend fun uploadImage(imageFile: File, destinationFileName: String): String? {
            return withContext(Dispatchers.IO) {
                try {
                    if (!imageFile.exists() || !imageFile.canRead()) {
                        Log.e(
                            "SupabaseStorage",
                            "Image file does not exist or is not readable: ${imageFile.absolutePath}"
                        )
                        return@withContext null
                    }

                    val bucketName = "images"
                    val byteArray = imageFile.readBytes()

                    Log.d("SupabaseStorageUploader", "Uploading image: $destinationFileName")
                    // Unggah file ke bucket
                    supabase.storage.from(bucketName).upload(destinationFileName, byteArray)

                    // Dapatkan URL publik
                    val publicUrl = supabase.storage.from(bucketName).publicUrl(destinationFileName)
                    Log.d(
                        "SupabaseStorageUploader",
                        "Image uploaded successfully. Public URL: $publicUrl"
                    )
                    publicUrl
                } catch (e: Exception) {
                    Log.e("SupabaseStorage", "Gagal mengunggah gambar: ${e.message}", e)
                    null
                }
            }
        }
    }
