package com.example.services

object ActiveChatSession {
    @Volatile
    var activeChatId: String? = null
}
