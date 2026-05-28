package com.example.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WaveFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("WaveFCM", "New FCM Token received: $token")
        
        // Ensure Firebase is initialized in service process
        try {
            FirebaseManager.initialize(applicationContext)
        } catch (e: Exception) {
            Log.e("WaveFCM", "Firebase initialization failed in onNewToken", e)
        }
        
        // Save token locally in SharedPreferences
        val prefs = getSharedPreferences("wave_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
        
        // If user is logged in, sync to Firestore
        try {
            val uid = FirebaseManager.auth.currentUser?.uid
            if (uid != null) {
                updateTokenInFirestore(uid, token)
            }
        } catch (e: Exception) {
            Log.e("WaveFCM", "Failed to access user session in onNewToken", e)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("WaveFCM", "Received FCM Message from: ${remoteMessage.from}")

        // Ensure Firebase is initialized in service process
        try {
            FirebaseManager.initialize(applicationContext)
        } catch (e: Exception) {
            Log.e("WaveFCM", "Firebase initialization failed in onMessageReceived", e)
        }

        // Try parsing data payload first
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val chatId = data["chatId"] ?: ""
            val senderId = data["senderId"] ?: ""
            val senderName = data["senderName"] ?: data["title"] ?: "New Message"
            val text = data["text"] ?: data["body"] ?: ""

            // Prevent duplicate notifications if chat is currently open
            if (chatId.isNotEmpty() && chatId == ActiveChatSession.activeChatId) {
                Log.d("WaveFCM", "Chat is already open ($chatId), ignoring notification.")
                return
            }

            showNotification(senderName, text, senderId, chatId)
        } else {
            // Fallback to standard notification payload
            remoteMessage.notification?.let {
                val title = it.title ?: "New Message"
                val body = it.body ?: ""
                showNotification(title, body, "", "")
            }
        }
    }

    private fun showNotification(title: String, body: String, senderId: String, chatId: String) {
        val channelId = "chat_messages"
        val notificationId = System.currentTimeMillis().toInt()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Wave chat messages"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("userId", senderId)
            putExtra("chatId", chatId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Using new wave logo for consistent branding
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(com.example.R.drawable.ic_wave_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun updateTokenInFirestore(uid: String, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseManager.db.collection("users").document(uid)
                    .update("fcmToken", token)
                Log.d("WaveFCM", "FCM Token updated successfully in Firestore.")
            } catch (e: Exception) {
                try {
                    FirebaseManager.db.collection("users").document(uid)
                        .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                    Log.d("WaveFCM", "FCM Token merged successfully in Firestore.")
                } catch (ex: Exception) {
                    Log.e("WaveFCM", "Failed to update FCM token in Firestore", ex)
                }
            }
        }
    }
}
