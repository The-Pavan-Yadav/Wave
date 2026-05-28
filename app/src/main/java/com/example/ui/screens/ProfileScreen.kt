package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.services.AppSettings
import com.example.viewmodels.AuthViewModel
import com.example.viewmodels.ProfileViewModel

import androidx.activity.ComponentActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
) {
    val profileViewModel: ProfileViewModel = viewModel()
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(
        viewModelStoreOwner = context as ComponentActivity
    )
    val profile by profileViewModel.profile.collectAsState()
    val isLoading by profileViewModel.isLoading.collectAsState()
    val updateError by profileViewModel.error.collectAsState()
    
    LaunchedEffect(updateError) {
        updateError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    val isUsernameChangeRestricted = remember(profile) {
        val lastChange = profile?.lastUsernameChange ?: 0L
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        System.currentTimeMillis() < (lastChange + thirtyDaysInMillis)
    }
    
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    
    var username by remember(profile) { mutableStateOf(profile?.username ?: "") }
    var displayName by remember(profile) { mutableStateOf(profile?.displayName ?: "") }
    var bio by remember(profile) { mutableStateOf(profile?.bio ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    var showSuccess by remember { mutableStateOf(value = false) }
    var showLogoutDialog by remember { mutableStateOf(value = false) }
    var isLoggingOut by remember { mutableStateOf(value = false) }
    
    // Theme & preferences states from AppSettings
    val notificationsEnabled by AppSettings.notificationsEnabled.collectAsState()

    val primaryGradient = Brush.linearGradient(
        colors = listOf(primaryColor, secondaryColor)
    )

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoggingOut) showLogoutDialog = false },
            title = {
                Text(
                    "Sign Out of Wave?",
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            },
            text = {
                Text(
                    "You will need to re-verify your credentials to sign back in.",
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoggingOut = true
                        authViewModel.logout { success ->
                            isLoggingOut = false
                            if (success) {
                                showLogoutDialog = false
                            } else {
                                android.widget.Toast.makeText(context, "Sign out failed. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isLoggingOut,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    if (isLoggingOut) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    enabled = !isLoggingOut
                ) {
                    Text("Cancel", color = textPrimary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Waves & Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Logout",
                            tint = Color(0xFFEF4444)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Success indicator
            AnimatedVisibility(
                visible = showSuccess,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF10B981).copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CheckCircle, "Success", tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Profile updated successfully!", color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Avatar picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(114.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 3.dp,
                            brush = primaryGradient,
                            shape = CircleShape
                        )
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (profile?.avatar?.isNotEmpty() == true) {
                        AsyncImage(
                            model = profile?.avatar,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = "Upload Avatar", tint = primaryColor, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("EDIT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fields Header
            Text("ACCOUNT INFORMATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(10.dp))

            // Inputs Row
            OutlinedTextField(
                value = username,
                onValueChange = { if (!isUsernameChangeRestricted) username = it.filter { char -> (char.isLetterOrDigit() || char == '_') } },
                label = { Text("Username") },
                maxLines = 1,
                enabled = !isUsernameChangeRestricted && !isLoading,
                leadingIcon = { Icon(Icons.Rounded.AlternateEmail, null, tint = if (isUsernameChangeRestricted) textSecondary.copy(alpha = 0.4f) else primaryColor) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    focusedLabelColor = primaryColor,
                    disabledTextColor = textSecondary.copy(alpha = 0.6f),
                    disabledLabelColor = textSecondary.copy(alpha = 0.5f),
                    disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Username can only be changed once every 30 days",
                fontSize = 11.sp,
                color = if (isUsernameChangeRestricted) primaryColor.copy(alpha = 0.7f) else textSecondary.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 14.dp, top = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                maxLines = 1,
                leadingIcon = { Icon(Icons.Rounded.Person, null, tint = primaryColor) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    focusedLabelColor = primaryColor
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio / Status message") },
                maxLines = 3,
                leadingIcon = { Icon(Icons.Rounded.Info, null, tint = primaryColor) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    focusedLabelColor = primaryColor
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(96.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(primaryGradient)
                    .clickable(enabled = !isLoading) {
                        showSuccess = false
                        profileViewModel.updateProfile(username, displayName, bio, selectedImageUri) { success ->
                            if (success) {
                                showSuccess = true
                                selectedImageUri = null
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = "Save", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile Changes", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Preferences Header
            Text("PREFERENCES & STYLE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // Preference card options
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Notifications enabled
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { AppSettings.setNotificationsEnabled(!notificationsEnabled) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (notificationsEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff, 
                                contentDescription = null, 
                                tint = primaryColor
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Push Notifications", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text(if (notificationsEnabled) "Active background delivery" else "Notifications paused", fontSize = 12.sp, color = textSecondary)
                            }
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { AppSettings.setNotificationsEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = primaryColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // About/Info section
            Text("ABOUT WAVE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Wave",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = textPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = "Verified build",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Secure realtime messaging experience crafted with passion, attention to detail, and smooth modern design.",
                        fontSize = 13.sp,
                        color = textSecondary.copy(alpha = 0.85f),
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(primaryColor.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Made by Pavan.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = primaryColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
