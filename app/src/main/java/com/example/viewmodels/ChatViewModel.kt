package com.example.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.services.FirebaseManager
import com.example.models.Message
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

import com.google.firebase.firestore.DocumentSnapshot
import com.example.models.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.example.models.ChatRoom

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _otherUser = MutableStateFlow<UserProfile?>(null)
    val otherUser: StateFlow<UserProfile?> = _otherUser.asStateFlow()

    private val _chatRoom = MutableStateFlow<ChatRoom?>(null)
    val chatRoom: StateFlow<ChatRoom?> = _chatRoom.asStateFlow()

    private val _isOtherTyping = MutableStateFlow(false)
    val isOtherTyping: StateFlow<Boolean> = _isOtherTyping.asStateFlow()

    private var messagesListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
    private var chatRoomListener: ListenerRegistration? = null
    var currentChatId: String? = null
        private set

    fun loadMessages(chatId: String, otherUserId: String) {
        if (currentChatId == chatId && chatRoomListener != null) return
        currentChatId = chatId
        
        _isOtherTyping.value = false
        messagesListener?.remove()
        userListener?.remove()
        chatRoomListener?.remove()
        
        try {
            chatRoomListener = FirebaseManager.db.collection("chats")
                .document(chatId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener
                    try {
                        val cr = snapshot.toObject(ChatRoom::class.java)
                        _chatRoom.value = cr
                        
                        // Real-time typing indicator from the other user
                        val otherTyping = cr?.typingStatus?.get(otherUserId) ?: false
                        _isOtherTyping.value = otherTyping
                    } catch (ex: Exception) {
                        Log.e("ChatViewModel", "Error parsing chatRoom", ex)
                    }
                }
            
            userListener = FirebaseManager.db.collection("users")
                .document(otherUserId)
                .addSnapshotListener { snapshot: DocumentSnapshot?, e: FirebaseFirestoreException? ->
                    if (e != null || snapshot == null) return@addSnapshotListener
                    try {
                        _otherUser.value = snapshot.toObject(UserProfile::class.java)
                    } catch (ex: Exception) {
                        Log.e("ChatViewModel", "Error parsing other user", ex)
                    }
                }
            
            messagesListener = FirebaseManager.db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot: QuerySnapshot?, e: FirebaseFirestoreException? ->
                    if (e != null) {
                        Log.e("ChatViewModel", "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        viewModelScope.launch(Dispatchers.Default) {
                            try {
                                val parsedMessages = mutableListOf<Message>()
                                val unreadDocRefs = mutableListOf<com.google.firebase.firestore.DocumentReference>()
                                val currentUserId = FirebaseManager.auth.currentUser?.uid ?: ""

                                for (doc in snapshot.documents) {
                                    val msg = doc.toObject(Message::class.java)
                                    if (msg != null) {
                                        parsedMessages.add(msg)
                                        if (msg.receiverId == currentUserId && !msg.seen) {
                                            unreadDocRefs.add(doc.reference)
                                        }
                                    }
                                }

                                _messages.value = parsedMessages

                                if (unreadDocRefs.isNotEmpty() && currentUserId.isNotEmpty()) {
                                    val batch = FirebaseManager.db.batch()
                                    for (ref in unreadDocRefs) {
                                        batch.update(ref, "seen", true)
                                    }
                                    batch.update(
                                        FirebaseManager.db.collection("chats").document(chatId),
                                        "unreadCounts.$currentUserId", 0
                                    )
                                    batch.commit()
                                }
                            } catch (ex: Exception) {
                                Log.e("ChatViewModel", "Error parsing messages", ex)
                            }
                        }
                    }
                }
        } catch (e: Throwable) {
            Log.e("ChatViewModel", "Failed to load messages safety wrapper triggered", e)
        }
    }

    fun sendMessage(chatId: String, receiverId: String, text: String) {
        val currentUserId = FirebaseManager.auth.currentUser?.uid ?: return
        if (text.isBlank()) return
        
        val docRef = FirebaseManager.db.collection("chats")
            .document(chatId)
            .collection("messages")
            .document()
        
        val message = Message(
            id = docRef.id,
            text = text.trim(),
            senderId = currentUserId,
            receiverId = receiverId,
            timestamp = System.currentTimeMillis()
        )
        
        // Ensure chats document exists to avoid orphaned subcollections issues
        val chatRef = FirebaseManager.db.collection("chats").document(chatId)
        val batch = FirebaseManager.db.batch()
        
        typingJob?.cancel()
        lastTypingStatus = Pair(chatId, false)
        
        val updateData = hashMapOf<String, Any>(
            "id" to chatId,
            "participants" to listOf(currentUserId, receiverId),
            "lastMessage" to message.text,
            "lastMessageTimestamp" to message.timestamp,
            "unreadCounts.$receiverId" to FieldValue.increment(1),
            "lastUpdated" to System.currentTimeMillis(),
            "typingStatus.$currentUserId" to false
        )
        batch.set(chatRef, updateData, SetOptions.merge())
        batch.set(docRef, message)
        batch.commit().addOnSuccessListener {
            FirebaseManager.db.collection("users").document(currentUserId).get()
                .addOnSuccessListener { doc ->
                    val senderName = doc.getString("displayName")?.takeIf { it.isNotBlank() }
                        ?: doc.getString("username") ?: "Wave User"
                    com.example.services.NotificationSender.sendNotification(
                        senderId = currentUserId,
                        senderName = senderName,
                        receiverId = receiverId,
                        chatId = chatId,
                        text = text.trim()
                    )
                }
        }.addOnFailureListener { e ->
            Log.e("ChatViewModel", "Failed to send message: ${e.message}")
        }
    }

    private var lastTypingStatus: Pair<String, Boolean>? = null
    private var typingJob: kotlinx.coroutines.Job? = null

    fun setLocalTyping(chatId: String, isBlank: Boolean) {
        if (isBlank) {
            typingJob?.cancel()
            updateTypingStatus(chatId, false)
            return
        }

        // Only start a new job if we aren't already typing or to reset the timer
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            updateTypingStatus(chatId, true)
            delay(2000) // Auto-stop after 2 seconds of inactivity
            updateTypingStatus(chatId, false)
        }
    }

    fun updateTypingStatus(chatId: String, isTyping: Boolean) {
        try {
            val currentUserId = FirebaseManager.auth.currentUser?.uid ?: return
            
            // Optimization: Don't write to Firestore if the status hasn't changed
            if (lastTypingStatus?.first == chatId && lastTypingStatus?.second == isTyping) return
            
            lastTypingStatus = Pair(chatId, isTyping)
            val chatRef = FirebaseManager.db.collection("chats").document(chatId)
            
            chatRef.update("typingStatus.$currentUserId", isTyping).addOnFailureListener {
                // If update fails (e.g. document doesn't exist), try set with merge
                chatRef.set(
                    mapOf("typingStatus" to mapOf(currentUserId to isTyping)),
                    SetOptions.merge()
                )
            }
        } catch (e: Throwable) {
            Log.e("ChatViewModel", "Failed to update typing status", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        typingJob?.cancel()
        messagesListener?.remove()
        userListener?.remove()
        chatRoomListener?.remove()
    }
}
