package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.models.UserProfile

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodels.HomeViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.material.icons.rounded.Close
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.animateContentSize

@Composable
fun HomeScreen(
    profile: UserProfile? = null,
    onNavigateToChat: (UserProfile) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
) {
    val homeViewModel: HomeViewModel = viewModel()
    val users by homeViewModel.users.collectAsState()
    val chats by homeViewModel.chats.collectAsState()
    val searchQuery by homeViewModel.searchQuery.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    var isSearchExpanded by remember { mutableStateOf(value = false) }
    var selectedTab by remember { mutableStateOf("Chats") }

    val currentUserId = profile?.uid ?: ""
    val totalUnreadCount = remember(chats, currentUserId) {
        chats.sumOf { it.unreadCounts?.get(currentUserId) ?: 0 }
    }

    val primaryGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF7C3AED), Color(0xFF2563EB))
    )
    val surfaceColor = Color(0xFF111827)
    val backgroundColor = Color(0xFF0B0F1A)
    val textPrimary = Color(0xFFF9FAFB)
    val textSecondary = Color(0xFF9CA3AF)
    val borderColor = Color.White.copy(alpha = 0.05f)

    Scaffold(
        containerColor = backgroundColor,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(surfaceColor.copy(alpha = 0.9f))
                    .border(1.dp, borderColor, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chat tab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedTab = "Chats" }
                    ) {
                        if (selectedTab == "Chats") {
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF7C3AED).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    Icon(Icons.Rounded.ChatBubble, contentDescription = "Chats", tint = Color(0xFF7C3AED))
                                    if (totalUnreadCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .offset(x = 6.dp, y = (-6).dp)
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color.Red),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (totalUnreadCount > 99) "99+" else totalUnreadCount.toString(),
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(Icons.Rounded.ChatBubble, contentDescription = "Chats", tint = textSecondary)
                                if (totalUnreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 6.dp, y = (-6).dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (totalUnreadCount > 99) "99+" else totalUnreadCount.toString(),
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("CHATS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == "Chats") Color(0xFF7C3AED) else textSecondary)
                    }
                    // People tab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedTab = "People" }
                    ) {
                        if (selectedTab == "People") {
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF7C3AED).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.People, contentDescription = "People", tint = Color(0xFF7C3AED))
                            }
                        } else {
                            Icon(Icons.Rounded.People, contentDescription = "People", tint = textSecondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("PEOPLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == "People") Color(0xFF7C3AED) else textSecondary)
                    }
                    // Settings tab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onNavigateToProfile() }
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("SETTINGS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .animateContentSize(tween(300)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearchExpanded) {
                    Box(modifier = Modifier.weight(1f).animateContentSize()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1F2937))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { homeViewModel.updateSearchQuery(it) },
                                textStyle = TextStyle(color = textPrimary, fontSize = 16.sp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                cursorBrush = SolidColor(Color(0xFF7C3AED)),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (searchQuery.isEmpty()) {
                                            Text("Search users...", color = textSecondary)
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable {
                                    isSearchExpanded = false
                                    homeViewModel.updateSearchQuery("")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = textSecondary)
                        }
                    }
                }
            }
                
                if (!isSearchExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_wave_logo),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Wave",
                                    style = TextStyle(
                                        brush = primaryGradient,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "MESSAGES",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textSecondary,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Search icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(surfaceColor)
                                    .border(1.dp, borderColor, CircleShape)
                                    .clickable { isSearchExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = textSecondary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFF7C3AED), CircleShape)
                                    .padding(2.dp)
                            ) {
                                if (profile != null && (profile.avatar.isNotEmpty())) {
                                    AsyncImage(
                                        model = profile.avatar,
                                        contentDescription = "Profile",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(Color(0xFF374151)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_wave_logo),
                                            contentDescription = "Default Avatar",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val displayUsers = remember(users, chats, selectedTab, searchQuery, currentUserId) {
                if (selectedTab == "Chats") {
                    if (searchQuery.isBlank()) {
                        users.filter { user ->
                            val chatId = if (currentUserId < user.uid) "${currentUserId}_${user.uid}" else "${user.uid}_$currentUserId"
                            val chat = chats.find { it.id == chatId }
                            chat != null && chat.lastUpdated > 0
                        }
                    } else {
                        // Advanced: search chats by username, displayName OR matching messages inside the conversation (lastMessage!) 
                        users.filter { user ->
                            val chatId = if (currentUserId < user.uid) "${currentUserId}_${user.uid}" else "${user.uid}_$currentUserId"
                            val chat = chats.find { it.id == chatId }
                            val nameMatches = user.username.contains(searchQuery, ignoreCase = true) || 
                                              user.displayName.contains(searchQuery, ignoreCase = true)
                            val messageMatches = chat != null && chat.lastMessage.contains(searchQuery, ignoreCase = true)
                            (nameMatches || messageMatches) && chat != null && chat.lastUpdated > 0
                        }
                    }
                } else {
                    users
                }
            }
            
            val sortedUsers = remember(displayUsers, chats, currentUserId) {
                displayUsers.sortedByDescending { user ->
                    val chatId = if (currentUserId < user.uid) "${currentUserId}_${user.uid}" else "${user.uid}_$currentUserId"
                    chats.find { it.id == chatId }?.lastUpdated ?: 0L
                }
            }

            // Chat List
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoading) {
                    items(5) {
                        ChatListItemSkeleton()
                    }
                } else if (users.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (searchQuery.isNotEmpty()) "No users found for '$searchQuery'." else "No other users found yet.", 
                                color = textSecondary, 
                                fontSize = 14.sp
                            )
                        }
                    }
                } else if (displayUsers.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (selectedTab == "Chats") "No active chats yet. Find someone in People to start a conversation!" else "No users found.",
                                color = textSecondary, 
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = sortedUsers.distinctBy { it.uid },
                        key = { _, user -> user.uid }
                    ) { _, user ->
                        val chatId = if (currentUserId < user.uid) "${currentUserId}_${user.uid}" else "${user.uid}_$currentUserId"
                        val chat = chats.find { it.id == chatId }
                        ChatListItemReal(
                            user = user,
                            unreadCount = chat?.unreadCounts?.get(currentUserId) ?: 0,
                            lastMessage = chat?.lastMessage ?: "",
                            lastMessageTimestamp = chat?.lastMessageTimestamp ?: 0L,
                            onClick = { onNavigateToChat(user) }
                        )
                    }
                }
                
                // Add some padding at the bottom so elements are not hidden by the FAB and BottomBar
                item {
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }
}

fun getRelativeTimeString(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 15 -> "just now"
        seconds < 60 -> "${seconds}s ago"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 4 -> "${days}d ago"
        else -> java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    }
}

@Composable
fun ChatListItemReal(
    user: UserProfile,
    unreadCount: Int,
    lastMessage: String,
    lastMessageTimestamp: Long,
    onClick: () -> Unit
) {
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    
    val timeString = if (lastMessageTimestamp > 0) {
        getRelativeTimeString(lastMessageTimestamp)
    } else if (user.lastSeen > 0) {
        "Seen " + getRelativeTimeString(user.lastSeen)
    } else "Offline"
    
    val isOnline = user.online || user.onlineStatus

    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, delayMillis = 100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val primaryGradient = Brush.linearGradient(
        colors = listOf(primaryColor, secondaryColor)
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp, 
                color = if (isOnline) primaryColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.04f), 
                shape = RoundedCornerShape(22.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1F2937))
                    .border(
                        width = if (isOnline) 2.dp else 0.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                primaryColor, 
                                secondaryColor.copy(alpha = glowAlpha), 
                                Color(0xFF10B981), 
                                primaryColor
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                if (user.avatar.isNotEmpty()) {
                    AsyncImage(
                        model = user.avatar,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(primaryColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_wave_logo),
                            contentDescription = "Default Avatar",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .offset(x = 1.dp, y = 1.dp)
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayName = user.displayName.takeIf { it.isNotBlank() } ?: user.username
                Text(displayName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                if (timeString.isNotEmpty()) {
                    Text(timeString, fontSize = 11.sp, color = if (unreadCount > 0) primaryColor else textSecondary, fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                lastMessage.ifEmpty { "Tap to send a direct message..." },
                fontSize = 13.sp,
                color = if (unreadCount > 0) textPrimary else textSecondary.copy(alpha = 0.85f),
                fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        if (unreadCount > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                    .clip(CircleShape)
                    .background(primaryGradient)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(unreadCount.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun ChatListItem(
    initials: String,
    name: String,
    time: String,
    message: String,
    unreadCount: Int,
    avatarGradient: Brush,
    isOnline: Boolean
) {
    val textPrimary = Color(0xFFF9FAFB)
    val textSecondary = Color(0xFF9CA3AF)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF111827))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(avatarGradient),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = 4.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Green)
                        .border(2.dp, Color(0xFF111827), CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                Text(time, fontSize = 11.sp, color = textSecondary)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                message,
                fontSize = 14.sp,
                color = textSecondary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        if (unreadCount > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF2563EB))))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(unreadCount.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun GroupChatListItem(
    name: String,
    time: String,
    sender: String,
    message: String,
    unreadCount: Int
) {
    val textPrimary = Color(0xFFF9FAFB)
    val textSecondary = Color(0xFF9CA3AF)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF111827))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .offset(x = (-8).dp, y = (-4).dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2563EB))
                    .border(1.dp, Color(0xFF111827), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("PT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Box(
                modifier = Modifier
                    .offset(x = 8.dp, y = 4.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF9333EA))
                    .border(1.dp, Color(0xFF111827), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("D", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                Text(time, fontSize = 11.sp, color = textSecondary)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row {
                Text(
                    sender,
                    fontSize = 14.sp,
                    color = Color(0xFF7C3AED)
                )
                Text(
                    message,
                    fontSize = 14.sp,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ChatListItemSkeleton() {
    val infiniteTransition = rememberInfiniteTransition()
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.Gray.copy(alpha = 0.1f),
            Color.Gray.copy(alpha = 0.3f),
            Color.Gray.copy(alpha = 0.1f)
        ),
        start = androidx.compose.ui.geometry.Offset(translateAnim - 200f, translateAnim - 200f),
        end = androidx.compose.ui.geometry.Offset(translateAnim + 200f, translateAnim + 200f)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF111827))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush)
            )
        }
    }
}
