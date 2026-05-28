package com.example.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.UserProfile
import com.example.services.FirebaseManager
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {
    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var profileListener: ListenerRegistration? = null
    
    init {
        loadProfile()
    }

    private fun loadProfile() {
        try {
            val uid = FirebaseManager.auth.currentUser?.uid ?: return
            profileListener = FirebaseManager.db.collection("users").document(uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("ProfileViewModel", "Error loading profile", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        try {
                            _profile.value = snapshot.toObject(UserProfile::class.java)
                        } catch (ex: Exception) {
                             Log.e("ProfileViewModel", "Error parsing profile", ex)
                        }
                    }
                }
        } catch (e: Throwable) {
            Log.e("ProfileViewModel", "Failed to load profile safety wrapper triggered", e)
        }
    }

    fun updateProfile(username: String, displayName: String, bio: String, newAvatarUri: Uri?, onComplete: (Boolean) -> Unit) {
        val uid = FirebaseManager.auth.currentUser?.uid ?: return
        val currentProfile = _profile.value
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to displayName,
                    "bio" to bio
                )
                
                // Username change logic
                if (currentProfile != null && username != currentProfile.username) {
                    val currentTime = System.currentTimeMillis()
                    val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
                    val nextAllowedChange = currentProfile.lastUsernameChange + thirtyDaysInMillis
                    
                    if (currentTime < nextAllowedChange) {
                        val remainingDays = ((nextAllowedChange - currentTime) / (24 * 60 * 60 * 1000)) + 1
                        _error.value = "You can change your username again in $remainingDays days"
                        _isLoading.value = false
                        onComplete(false)
                        return@launch
                    }

                    // Check if username is taken
                    val isTaken = FirebaseManager.db.collection("users")
                        .whereEqualTo("username_lowercase", username.lowercase())
                        .get()
                        .await()
                        .isEmpty.not()
                    
                    if (isTaken) {
                        _error.value = "Username is already taken"
                        _isLoading.value = false
                        onComplete(false)
                        return@launch
                    }

                    updates["username"] = username
                    updates["username_lowercase"] = username.lowercase()
                    updates["lastUsernameChange"] = currentTime
                }
                
                if (newAvatarUri != null) {
                    val timestamp = System.currentTimeMillis()
                    val storageRef = FirebaseManager.storage.reference.child("avatars/${uid}_${timestamp}.jpg")
                    storageRef.putFile(newAvatarUri).await()
                    val downloadUrl = storageRef.downloadUrl.await()
                    updates["avatar"] = downloadUrl.toString()
                }
                
                FirebaseManager.db.collection("users").document(uid).update(updates).await()
                _isLoading.value = false
                onComplete(true)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating profile", e)
                _isLoading.value = false
                onComplete(false)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        profileListener?.remove()
    }
}
