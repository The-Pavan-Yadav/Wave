package com.example.models

data class ChatRoom(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0,
    val lastUpdated: Long = 0,
    val unreadCounts: Map<String, Int> = emptyMap(),
    val typingStatus: Map<String, Boolean> = emptyMap()
)
