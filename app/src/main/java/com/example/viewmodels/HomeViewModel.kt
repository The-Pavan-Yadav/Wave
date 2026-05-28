package com.example.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.services.FirebaseManager
import com.example.models.UserProfile
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

import com.example.models.ChatRoom

class HomeViewModel : ViewModel() {
    private val _allUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    
    private val _chats = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chats: StateFlow<List<ChatRoom>> = _chats.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _debouncedSearchQuery = MutableStateFlow("")
    val debouncedSearchQuery: StateFlow<String> = _debouncedSearchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val users: StateFlow<List<UserProfile>> = combine(_allUsers, _debouncedSearchQuery) { all, query ->
        if (query.isBlank()) {
            all
        } else {
            all.filter { 
                it.username.contains(query, ignoreCase = true) || 
                it.displayName.contains(query, ignoreCase = true) ||
                it.bio.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var usersListener: ListenerRegistration? = null
    private var chatsListener: ListenerRegistration? = null
    private var searchDebounceJob: kotlinx.coroutines.Job? = null

    init {
        fetchUsers()
        fetchChats()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            kotlinx.coroutines.delay(250) // Highly optimized short debounce for typing comfort
            _debouncedSearchQuery.value = query
        }
    }

    private fun fetchChats() {
        try {
            val currentUserId = FirebaseManager.auth.currentUser?.uid ?: return
            chatsListener = FirebaseManager.db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener { snapshot: QuerySnapshot?, e: FirebaseFirestoreException? ->
                    if (e != null) {
                        Log.e("HomeViewModel", "Chats Listen failed.", e)
                        return@addSnapshotListener
                    }

                    try {
                        if (snapshot != null) {
                            val chatsList = snapshot.toObjects(ChatRoom::class.java)
                            _chats.value = chatsList.sortedByDescending { it.lastUpdated }
                        }
                    } catch (ex: Exception) {
                        Log.e("HomeViewModel", "Error parsing chats", ex)
                    }
                }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Failed to fetch chats", e)
        }
    }

    private fun fetchUsers() {
        try {
            val currentUserId = FirebaseManager.auth.currentUser?.uid ?: return
            usersListener = FirebaseManager.db.collection("users")
                .addSnapshotListener { snapshot: QuerySnapshot?, e: FirebaseFirestoreException? ->
                    _isLoading.value = false
                    if (e != null) {
                        Log.e("HomeViewModel", "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    try {
                        if (snapshot != null) {
                            val usersList = snapshot.toObjects(UserProfile::class.java)
                                .filter { it.uid != currentUserId }
                            _allUsers.value = usersList
                        }
                    } catch (ex: Exception) {
                        Log.e("HomeViewModel", "Error parsing users", ex)
                    }
                }
        } catch (e: Exception) {
            _isLoading.value = false
            Log.e("HomeViewModel", "Failed to fetch users", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        usersListener?.remove()
        chatsListener?.remove()
    }
}
