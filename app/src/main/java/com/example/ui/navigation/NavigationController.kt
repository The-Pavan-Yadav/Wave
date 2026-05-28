package com.example.ui.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NavigationController {
    private val _navEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navEvents = _navEvents.asSharedFlow()

    @Volatile
    var pendingUserId: String? = null

    fun navigateTo(route: String) {
        _navEvents.tryEmit(route)
    }
}
