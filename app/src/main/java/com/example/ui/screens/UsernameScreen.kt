package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun UsernameScreen(
    onSubmitUsername: (String) -> Unit,
    isLoading: Boolean = false
) {
    var username by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val backgroundColor = Color(0xFF0B0F1A)
    val surfaceColor = Color(0xFF111827)
    val primaryColor = Color(0xFF7C3AED)
    val secondaryColor = Color(0xFF2563EB)
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
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                "Create a profile",
                color = textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "How should people call you?",
                color = textSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Avatar Preview
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(surfaceColor)
                    .border(2.dp, primaryColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (username.isNotEmpty()) {
                    val encodedUsername = java.net.URLEncoder.encode(username, "UTF-8")
                    AsyncImage(
                        model = "https://api.dicebear.com/7.x/bottts/png?seed=${encodedUsername}",
                        contentDescription = "Avatar Preview",
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("?", color = textSecondary, fontSize = 32.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it.take(20) },
                placeholder = { Text("Username", color = textSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    cursorColor = primaryColor
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { 
                        keyboardController?.hide()
                        if (!isLoading && username.length >= 3) {
                            onSubmitUsername(username)
                        }
                    }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (username.length >= 3) {
                            Brush.linearGradient(listOf(primaryColor, secondaryColor))
                        } else {
                            Brush.linearGradient(listOf(surfaceColor, surfaceColor))
                        }
                    )
                    .clickable(enabled = !isLoading && username.length >= 3) {
                        keyboardController?.hide()
                        onSubmitUsername(username)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        "Continue",
                        color = if (username.length >= 3) Color.White else textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
