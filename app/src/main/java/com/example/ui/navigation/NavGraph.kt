package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.UsernameScreen
import com.example.viewmodels.AuthState
import com.example.viewmodels.AuthViewModel

import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@Composable
fun WaveApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    val isProcessing by authViewModel.isProcessing.collectAsState()
    val context = LocalContext.current

    // Observe global programmatic navigation triggers
    LaunchedEffect(Unit) {
        NavigationController.navEvents.collect { route ->
            try {
                if (authViewModel.authState.value is AuthState.Authenticated) {
                    navController.navigate(route)
                }
            } catch (e: Throwable) {
                android.util.Log.e("WaveApp", "Programmatic navigation failed for $route", e)
            }
        }
    }

    // Handle global logout navigation safely
    LaunchedEffect(authState) {
        try {
            android.util.Log.d("WaveApp", "Auth state changed: $authState")
            if (authState is AuthState.Unauthenticated) {
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute != "auth" && currentRoute != "splash") {
                    android.util.Log.i("WaveApp", "Redirecting to Auth screen. Source: $currentRoute")
                    navController.navigate("auth") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("WaveApp", "Logout navigation failed", e)
            Toast.makeText(context, "Sign out navigation failed", Toast.LENGTH_SHORT).show()
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = Modifier.fillMaxSize(),
        enterTransition = { fadeIn(animationSpec = tween(400)) },
        exitTransition = { fadeOut(animationSpec = tween(400)) },
        popEnterTransition = { fadeIn(animationSpec = tween(400)) },
        popExitTransition = { fadeOut(animationSpec = tween(400)) },
    ) {
        composable("splash") {
            var splashFinished by remember { mutableStateOf(value = false) }
            
            SplashScreen { splashFinished = true }
            
            // Watch for both splash finishing and auth state resolving
            LaunchedEffect(authState, splashFinished) {
                if (splashFinished && (authState !is AuthState.Initializing)) {
                    try {
                        when (authState) {
                            is AuthState.Authenticated -> {
                                val pendingUid = NavigationController.pendingUserId
                                if (!pendingUid.isNullOrEmpty()) {
                                    NavigationController.pendingUserId = null
                                    navController.navigate("home") { popUpTo(navController.graph.id) { inclusive = true } }
                                    navController.navigate("chat/$pendingUid")
                                } else {
                                    navController.navigate("home") { popUpTo(navController.graph.id) { inclusive = true } }
                                }
                            }
                            is AuthState.NeedsUsername -> navController.navigate("username") { popUpTo(navController.graph.id) { inclusive = true } }
                            else -> navController.navigate("auth") { popUpTo(navController.graph.id) { inclusive = true } }
                        }
                    } catch (e: Throwable) {
                        android.util.Log.e("WaveApp", "Splash completed navigation failed", e)
                    }
                }
            }
        }
        
        composable("auth") {
            // Watch for successful auth
            LaunchedEffect(authState) {
                try {
                    if (authState is AuthState.NeedsUsername) {
                        navController.navigate("username") { popUpTo(navController.graph.id) { inclusive = true } }
                    } else if (authState is AuthState.Authenticated) {
                        val pendingUid = NavigationController.pendingUserId
                        if (!pendingUid.isNullOrEmpty()) {
                            NavigationController.pendingUserId = null
                            navController.navigate("home") { popUpTo(navController.graph.id) { inclusive = true } }
                            navController.navigate("chat/$pendingUid")
                        } else {
                            navController.navigate("home") { popUpTo(navController.graph.id) { inclusive = true } }
                        }
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("WaveApp", "Auth navigation handler failed", e)
                }
            }

            val errorMessage = (authState as? AuthState.Error)?.message
            
            AuthScreen(
                onGoogleLoginSuccess = { idToken -> authViewModel.loginWithGoogle(idToken) },
                onEmailLogin = { email, pass -> authViewModel.loginWithEmail(email, pass) },
                onRegister = { email, pass, username -> authViewModel.registerWithEmail(email, pass, username) },
                errorMessage = errorMessage,
                isLoading = isProcessing || authState is AuthState.Initializing,
            )
        }

        composable("username") {
            LaunchedEffect(authState) {
                try {
                    if (authState is AuthState.Authenticated) {
                        navController.navigate("home") { popUpTo(navController.graph.id) { inclusive = true } }
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("WaveApp", "Username screen navigation handler failed", e)
                }
            }
            
            UsernameScreen(
                onSubmitUsername = { authViewModel.createUsername(it) },
                isLoading = isProcessing || authState is AuthState.Initializing,
            )
        }
        
        composable("home") {
            val profile = (authState as? AuthState.Authenticated)?.profile
            HomeScreen(
                profile = profile,
                onNavigateToChat = { user ->
                    try {
                        navController.navigate("chat/${user.uid}")
                    } catch (e: Throwable) {
                        android.util.Log.e("WaveApp", "Failed to navigate to Chat Screen represent user", e)
                    }
                },
                onNavigateToProfile = {
                    try {
                        navController.navigate("profile")
                    } catch (e: Throwable) {
                        android.util.Log.e("WaveApp", "Failed to navigate to Profile Screen", e)
                    }
                }
            )
        }
        composable(
            route = "chat/{userId}",
            arguments = listOf(
                androidx.navigation.navArgument("userId") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            
            com.example.ui.screens.ChatScreen(
                userId = userId,
                onBack = {
                    try {
                        navController.popBackStack()
                    } catch (e: Throwable) {
                        android.util.Log.e("WaveApp", "Failed to pop backstack from chat", e)
                    }
                }
            )
        }
        composable("profile") {
            com.example.ui.screens.ProfileScreen(
                onBack = {
                    try {
                        navController.popBackStack()
                    } catch (e: Throwable) {
                        android.util.Log.e("WaveApp", "Failed to pop backstack from profile", e)
                    }
                }
            )
        }
    }
}
