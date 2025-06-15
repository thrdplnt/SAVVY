package com.example.savvy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "SavvyFirebaseMsgSvc"
        private const val CHANNEL_ID = "savvy_notifications"
    }

    /**
     * Dipanggil saat token baru dibuat atau diperbarui.
     * Di sinilah kita menyimpan token ke Firestore agar server bisa mengirim notifikasi
     * ke perangkat ini secara spesifik.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        // Panggil fungsi untuk mengirim token ke server/database Anda
        sendRegistrationToServer(token)
    }

    /**
     * Menyimpan FCM token ke Firestore di bawah dokumen pengguna yang sedang login.
     * Nama koleksi: "fcm_tokens"
     * Nama dokumen: UID Pengguna
     */
    private fun sendRegistrationToServer(token: String?) {
        if (token == null) return

        // Dapatkan userId dari pengguna yang sedang login saat ini
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            // Data yang akan disimpan adalah token itu sendiri
            val tokenData = hashMapOf("token" to token)

            // Simpan token ke koleksi 'fcm_tokens' dengan ID dokumen adalah userId pengguna.
            // .set() akan membuat dokumen baru jika belum ada, atau menimpanya jika sudah ada.
            db.collection("fcm_tokens").document(userId)
                .set(tokenData)
                .addOnSuccessListener { Log.d(TAG, "FCM token saved to Firestore for user: $userId") }
                .addOnFailureListener { e -> Log.w(TAG, "Error saving FCM token", e) }
        } else {
            // Jika pengguna belum login saat token dibuat, token ini belum bisa disimpan.
            // Penyimpanan akan dicoba lagi saat pengguna login atau saat token di-refresh di lain waktu.
            Log.w(TAG, "Cannot save FCM token, user is not logged in.")
        }
    }

    /**
     * Menerima notifikasi yang masuk.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Handle notifikasi yang memiliki payload 'notification'
        remoteMessage.notification?.let {
            val title = it.title ?: "Savvy Notification"
            val body = it.body ?: "You have a new message."
            Log.d(TAG, "Message Notification Title: $title")
            Log.d(TAG, "Message Notification Body: $body")

            // Tampilkan notifikasi di bar status perangkat
            showNotification(title, body)
        }
    }

    /**
     * Membuat dan menampilkan notifikasi sederhana.
     */
    private fun showNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Buat Notification Channel untuk Android Oreo (API 26) ke atas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Savvy Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel untuk notifikasi aplikasi Savvy"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Pastikan ikon ini ada
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Notifikasi akan hilang saat di-tap

        notificationManager.notify(1, notificationBuilder.build())
    }
}
