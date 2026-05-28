package com.example.services

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object NotificationSender {
    private val client = OkHttpClient()

    fun sendNotification(
        senderId: String,
        senderName: String,
        receiverId: String,
        chatId: String,
        text: String
    ) {
        val fstore = FirebaseFirestore.getInstance()
        
        // 1. Write metadata to default Firestore notifications collection for backend triggers / cloud integrations
        val notificationData = hashMapOf(
            "senderId" to senderId,
            "senderName" to senderName,
            "receiverId" to receiverId,
            "chatId" to chatId,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )
        
        fstore.collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                Log.d("NotificationSender", "Notification stored in Firestore queue successfully.")
            }
            .addOnFailureListener { e ->
                Log.e("NotificationSender", "Failed to store notification in Firestore queue", e)
            }

        // 2. Fetch the recipient's FCM registration token from Firestore and push directly
        fstore.collection("users").document(receiverId).get()
            .addOnSuccessListener { doc ->
                val fcmToken = doc.getString("fcmToken")
                if (!fcmToken.isNullOrEmpty()) {
                    triggerFcmPush(fcmToken, senderName, text, chatId, senderId)
                } else {
                    Log.d("NotificationSender", "Target user $receiverId has no FCM registration token.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("NotificationSender", "Failed to resolve recipient user FCM token from Firestore", e)
            }
    }

    private fun triggerFcmPush(
        targetToken: String,
        senderName: String,
        text: String,
        chatId: String,
        senderId: String
    ) {
        val jsonPayload = """
            {
                "to": "$targetToken",
                "priority": "high",
                "data": {
                    "title": "$senderName",
                    "body": "$text",
                    "chatId": "$chatId",
                    "senderId": "$senderId",
                    "senderName": "$senderName",
                    "text": "$text",
                    "type": "chat_message"
                }
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toRequestBody(mediaType)

        // Standard default server key for background client-to-client operations
        // NOTE: In production, this should NEVER be on the client. Use Firebase Cloud Functions.
        val serverKey = "AAAA3uM-VGs:APA91bF2b8yX7LqZ4y37_2N9S8m823_Yh1o9_placeholder"

        val request = Request.Builder()
            .url("https://fcm.googleapis.com/fcm/send")
            .post(requestBody)
            .addHeader("Authorization", "key=$serverKey")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("NotificationSender", "FCM REST direct HTTP call failed", e)
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.w("NotificationSender", "FCM REST direct call unsucceeded: HTTP ${response.code}")
                    } else {
                        Log.d("NotificationSender", "FCM REST direct notification issued successfully.")
                    }
                }
            }
        })
    }
}
