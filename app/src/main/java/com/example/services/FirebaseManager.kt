package com.example.services

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object FirebaseManager {
    private var isFirestoreSettingsSet = false

    fun initialize(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyAPxAGeJGDBAu3Bv_FMuu1k_5UXXvcLGnY")
                    .setApplicationId("1:73702664175:android:68b90b53ea592901e6381a")
                    .setProjectId("wave-6a491")
                    .setStorageBucket("wave-6a491.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.d("FirebaseManager", "Firebase manually initialized.")
            }
            
            if (!isFirestoreSettingsSet) {
                try {
                    // Set Firestore settings globally safely
                    val firestore = FirebaseFirestore.getInstance()
                    val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build())
                        .build()
                    firestore.firestoreSettings = settings
                } catch (settVal: Throwable) {
                    Log.w("FirebaseManager", "Firestore settings could not be globally applied because Firestore was already active.", settVal)
                }
                isFirestoreSettingsSet = true
            }
            
        } catch (e: Throwable) {
            Log.e("FirebaseManager", "Failed to initialize Firebase: ${e.message}", e)
        }
    }

    private fun ensureInitialized() {
        try {
            val app = FirebaseApp.getInstance()
        } catch (e: Throwable) {
            // Not initialized
        }
    }

    val auth: FirebaseAuth
        get() = try { FirebaseAuth.getInstance() } catch(e: Throwable) { throw IllegalStateException("Firebase not initialized", e) }

    val db: FirebaseFirestore
        get() = try { FirebaseFirestore.getInstance() } catch(e: Throwable) { throw IllegalStateException("Firebase not initialized", e) }

    val storage: FirebaseStorage
        get() = try { FirebaseStorage.getInstance() } catch(e: Throwable) { throw IllegalStateException("Firebase not initialized", e) }
}
