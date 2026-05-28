package com.example.models

import androidx.compose.runtime.Immutable

@Immutable
data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val username_lowercase: String = "",
    val email: String = "",
    val avatar: String = "",
    val createdAt: Long = 0,
    val online: Boolean = false,
    val onlineStatus: Boolean = false,
    val lastSeen: Long = 0,
    val lastUsernameChange: Long = 0
)
