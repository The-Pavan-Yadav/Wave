package com.example.ui.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.example.R

enum class AuthMode {
    Login, Register
}

@Composable
fun AuthScreen(
    onGoogleLoginSuccess: (String) -> Unit = {},
    onEmailLogin: (String, String) -> Unit = { _, _ -> },
    onRegister: (String, String, String) -> Unit = { _, _, _ -> },
    errorMessage: String? = null,
    isLoading: Boolean = false
) {
    val context = LocalContext.current
    var authMode by remember { mutableStateOf(AuthMode.Login) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val displayError = localError ?: errorMessage

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                account?.idToken?.let { idToken ->
                    onGoogleLoginSuccess(idToken)
                }
            } catch (e: Exception) {
                Log.e("AuthScreen", "Google sign in failed", e)
            }
        }
    }

    val backgroundColor = Color(0xFF0B0F1A)
    val surfaceColor = Color(0xFF111827)
    val primaryColor = Color(0xFF7C3AED)
    val textPrimary = Color(0xFFF9FAFB)
    val textSecondary = Color(0xFF9CA3AF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp).fillMaxWidth()
        ) {
            Text(
                text = if (authMode == AuthMode.Login) "Welcome Back" else "Create Account",
                color = textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (authMode == AuthMode.Login) "Secure realtime messaging" else "Join the secure messaging network",
                color = textSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(targetState = authMode, label = "auth_mode") { mode ->
                when (mode) {
                    AuthMode.Login -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Google Login
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(surfaceColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                    .clickable(enabled = !isLoading) { 
                                        try {
                                            val defaultClientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                                            val clientId = if (defaultClientIdRes != 0) context.getString(defaultClientIdRes) else ""
                                            val fallbackClientId = context.getString(R.string.google_web_client_id)
                                            val finalClientId = if (clientId.isNotEmpty() && clientId != "YOUR_CLIENT_ID") clientId else fallbackClientId
                                            
                                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                                .requestIdToken(finalClientId)
                                                .requestEmail()
                                                .build()
                                                
                                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                            googleSignInClient.signOut().addOnCompleteListener {
                                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                            }
                                        } catch (e: Exception) {
                                            Log.e("AuthScreen", "Google Sign-In failed", e)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Continue with Google", color = textPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text("OR", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email", color = textSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    cursorColor = primaryColor
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                enabled = !isLoading
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password", color = textSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    cursorColor = primaryColor
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                enabled = !isLoading
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { 
                                    keyboardController?.hide()
                                    localError = null
                                    if (!isLoading) {
                                        if (email.isBlank() || password.isBlank()) {
                                            localError = "Please fill in all fields."
                                            return@Button
                                        }
                                        onEmailLogin(email, password)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Login with Email", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text(
                                "Don't have an account? Create Account",
                                color = primaryColor,
                                modifier = Modifier.clickable(enabled = !isLoading) { 
                                    localError = null
                                    authMode = AuthMode.Register 
                                },
                                fontSize = 14.sp
                            )
                        }
                    }
                    AuthMode.Register -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username", color = textSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    cursorColor = primaryColor
                                ),
                                enabled = !isLoading
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email", color = textSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    cursorColor = primaryColor
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                enabled = !isLoading
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password", color = textSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    cursorColor = primaryColor
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                enabled = !isLoading
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm Password", color = textSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    cursorColor = primaryColor
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                enabled = !isLoading
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { 
                                    keyboardController?.hide()
                                    localError = null
                                    if (!isLoading) {
                                        if (username.isBlank() || email.isBlank() || password.isBlank()) {
                                            localError = "Please fill in all fields."
                                            return@Button
                                        }
                                        if (password != confirmPassword) {
                                            localError = "Passwords do not match."
                                            return@Button
                                        }
                                        onRegister(email, password, username)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Create Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text(
                                "Already have an account? Login with Email",
                                color = primaryColor,
                                modifier = Modifier.clickable(enabled = !isLoading) { 
                                    localError = null
                                    authMode = AuthMode.Login 
                                },
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = displayError != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (displayError != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = displayError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Text(
            text = "Made by Pavan",
            color = textSecondary.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        )
    }
}
