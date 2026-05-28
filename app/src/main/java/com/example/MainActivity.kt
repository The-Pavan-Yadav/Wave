package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.services.FirebaseManager
import com.example.ui.navigation.WaveApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.navigation.NavigationController

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
    private var presenceObserver: LifecycleEventObserver? = null

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "Notification permission granted.")
            syncFcmToken()
        } else {
            android.util.Log.w("MainActivity", "Notification permission denied.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase safely
        try {
            FirebaseManager.initialize(this)
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Firebase init error", e)
        }

        // Handle incoming notification intent at startup
        handleNotificationIntent(intent)
        
        presenceObserver = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
            try {
                val uid = FirebaseManager.auth.currentUser?.uid ?: return@LifecycleEventObserver
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        FirebaseManager.db.collection("users").document(uid).set(
                            mapOf(
                                "onlineStatus" to true,
                                "online" to true
                            ),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                        // Trigger token synchronization automatically
                        syncFcmToken()
                    }
                    Lifecycle.Event.ON_STOP -> {
                        FirebaseManager.db.collection("users").document(uid).set(
                            mapOf(
                                "onlineStatus" to false,
                                "online" to false,
                                "lastSeen" to System.currentTimeMillis()
                            ),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                    }
                    else -> {}
                }
            } catch (e: Throwable) {
                android.util.Log.e("MainActivity", "Presence update failed", e)
            }
        }
        lifecycle.addObserver(presenceObserver!!)

        // Request notification permission for Android 13+ properly
        requestNotificationPermission()

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var error by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
                    
                    if (error != null) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Text("App Error: $error\nPlease restart.")
                        }
                    } else {
                        WaveApp()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        try {
            if (intent == null) return
            val openUserId = intent.getStringExtra("userId")
            if (!openUserId.isNullOrEmpty()) {
                android.util.Log.d("MainActivity", "Notification tapped. Heading to user: $openUserId")
                NavigationController.pendingUserId = openUserId
                NavigationController.navigateTo("chat/$openUserId")
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error handling notification intent", e)
        }
    }

    private fun requestNotificationPermission() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (!hasPermission) {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error requesting notification permission", e)
        }
    }

    private fun syncFcmToken() {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                try {
                    if (task.isSuccessful) {
                        val token = task.result
                        val uid = FirebaseManager.auth.currentUser?.uid
                        if (uid != null && !token.isNullOrEmpty()) {
                            FirebaseManager.db.collection("users").document(uid)
                                .update("fcmToken", token)
                                .addOnFailureListener {
                                    FirebaseManager.db.collection("users").document(uid)
                                        .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                                }
                        }
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("MainActivity", "Error inside syncFcmToken callback", e)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Failed to sync FCM Token", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenceObserver?.let { lifecycle.removeObserver(it) }
    }
}
