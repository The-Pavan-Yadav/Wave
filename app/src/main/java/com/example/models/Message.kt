package com.example.models

import androidx.compose.runtime.Immutable

@Immutable
data class Message(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val timestamp: Long = 0,
    val seen: Boolean = false
)
