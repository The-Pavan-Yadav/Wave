package com.example.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.UserProfile
import com.example.services.FirebaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Initializing : AuthState()
    object Unauthenticated : AuthState()
    object NeedsUsername : AuthState()       // Authenticated, but no profile
    data class Authenticated(val profile: UserProfile) : AuthState() // Fully set up
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isProcessing = MutableStateFlow(value = false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        try {
            val currentUser = FirebaseManager.auth.currentUser
            if (currentUser != null) {
                if (currentUser.isAnonymous) {
                    // We removed guest login. Force sign out if an old guest session exists.
                    logout()
                } else {
                    checkUserProfile(currentUser.uid)
                }
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "checkAuthState error: ${e.message}")
            _authState.value = AuthState.Error("Initialization failed: ${e.message}")
        }
    }

    fun loginWithGoogle(idToken: String) {
        _isProcessing.value = true
        _authState.value = AuthState.Unauthenticated // Clear any previous errors
        viewModelScope.launch {
            try {
                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                val result = FirebaseManager.auth.signInWithCredential(credential).await()
                val user = result.user
                if (user != null) {
                    checkUserProfile(user.uid)
                } else {
                    _authState.value = AuthState.Error("Google login failed (no user found).")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login error: ${e.message}")
                _authState.value = AuthState.Error(e.message ?: "Google Login failed.")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun loginWithEmail(email: String, pass: String) {
        _isProcessing.value = true
        _authState.value = AuthState.Unauthenticated // Clear any previous errors
        viewModelScope.launch {
            try {
                val result = FirebaseManager.auth.signInWithEmailAndPassword(email, pass).await()
                val user = result.user
                if (user != null) {
                    checkUserProfile(user.uid)
                } else {
                    _authState.value = AuthState.Error("Login failed (no user found).")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Email Login error: ${e.message}")
                _authState.value = AuthState.Error(e.message ?: "Login failed.")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun registerWithEmail(email: String, pass: String, username: String) {
        _isProcessing.value = true
        _authState.value = AuthState.Unauthenticated // Clear any previous errors
        viewModelScope.launch {
            try {
                val result = FirebaseManager.auth.createUserWithEmailAndPassword(email, pass).await()
                val user = result.user
                if (user != null) {
                    // Pre-create the username/profile for them so it skips NeedsUsername
                    val encodedUsername = java.net.URLEncoder.encode(username, "UTF-8")
                    val avatarUrl = "https://api.dicebear.com/7.x/bottts/png?seed=$encodedUsername"
                    val profile = UserProfile(
                        uid = user.uid,
                        username = username,
                        username_lowercase = username.lowercase(),
                        email = user.email ?: "",
                        avatar = avatarUrl,
                        createdAt = System.currentTimeMillis(),
                        online = true,
                        onlineStatus = true,
                        lastSeen = System.currentTimeMillis()
                    )
                    FirebaseManager.db.collection("users").document(user.uid).set(profile).await()
                    _authState.value = AuthState.Authenticated(profile = profile)
                } else {
                    _authState.value = AuthState.Error("Registration failed.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Register error: ${e.message}")
                _authState.value = AuthState.Error(e.message ?: "Registration failed.")
                // Rollback user creation on db failure
                try {
                    FirebaseManager.auth.currentUser?.delete()?.await()
                } catch(deleteError: Exception) {
                    Log.e("AuthViewModel", "Rollback delete error: ${deleteError.message}")
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun checkUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val doc = FirebaseManager.db.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val profile = doc.toObject(UserProfile::class.java)
                    if (profile != null) {
                        FirebaseManager.db.collection("users").document(uid)
                            .update(
                                mapOf(
                                    "onlineStatus" to true,
                                    "lastSeen" to System.currentTimeMillis()
                                )
                            )
                        _authState.value = AuthState.Authenticated(profile.copy(onlineStatus = true, lastSeen = System.currentTimeMillis()))
                    } else {
                        _authState.value = AuthState.NeedsUsername
                    }
                } else {
                    _authState.value = AuthState.NeedsUsername
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Profile fetch error: ${e.message}")
                // If it fails because of missing configure, it's safer to prompt for NeedsUsername or Error
                _authState.value = AuthState.Error(e.message ?: "Failed to fetch user profile")
            }
        }
    }

    fun createUsername(username: String) {
        val uid = FirebaseManager.auth.currentUser?.uid ?: return
        _isProcessing.value = true
        _authState.value = AuthState.NeedsUsername

        viewModelScope.launch {
            try {
                // Determine avatar
                val encodedUsername = java.net.URLEncoder.encode(username, "UTF-8")
                val avatarUrl = "https://api.dicebear.com/7.x/bottts/png?seed=$encodedUsername"
                
                val profile = UserProfile(
                    uid = uid,
                    username = username,
                    username_lowercase = username.lowercase(),
                    email = FirebaseManager.auth.currentUser?.email ?: "",
                    avatar = avatarUrl,
                    createdAt = System.currentTimeMillis(),
                    online = true,
                    onlineStatus = true,
                    lastSeen = System.currentTimeMillis()
                )

                FirebaseManager.db.collection("users")
                    .document(uid)
                    .set(profile)
                    .await()

                _authState.value = AuthState.Authenticated(profile)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Username save error: ${e.message}")
                _authState.value = AuthState.Error(e.message ?: "Failed to save username")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun logout(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                // Get UID before signing out
                val uid = FirebaseManager.auth.currentUser?.uid
                
                if (uid != null) {
                    // Try to update online status but don't let it block the logout if it takes too long
                    try {
                        kotlinx.coroutines.withTimeoutOrNull(2000) {
                            FirebaseManager.db.collection("users").document(uid)
                                .update("onlineStatus", false)
                                .await()
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Status update failed during logout: ${e.message}")
                    }
                }
                
                FirebaseManager.auth.signOut()
                _authState.value = AuthState.Unauthenticated
                onComplete(true)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Logout error: ${e.message}")
                // Ensure we at least sign out locally
                FirebaseManager.auth.signOut()
                _authState.value = AuthState.Unauthenticated
                onComplete(true) // Return true because we are now "unauthenticated"
            }
        }
    }
}
