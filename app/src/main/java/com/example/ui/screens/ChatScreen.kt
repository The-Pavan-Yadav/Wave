package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.services.FirebaseManager
import com.example.models.Message
import com.example.viewmodels.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

fun getGroupedDateSeparator(timestamp: Long): String {
    val date = Date(timestamp)
    val today = Calendar.getInstance()
    val calendar = Calendar.getInstance().apply { time = date }
    
    return when {
         today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
         today.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR) -> "Today"
         
         today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
         today.get(Calendar.DAY_OF_YEAR) - calendar.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
         
         else -> SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date)
    }
}

@Composable
fun EmptyChatPlaceholder(
    isOtherTypingProvider: () -> Boolean,
    messagesEmptyProvider: () -> Boolean,
    modifier: Modifier = Modifier
) {
    val isOtherTyping = isOtherTypingProvider()
    val isEmpty = messagesEmptyProvider()
    if (isEmpty && !isOtherTyping) {
        Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.size(100.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Say hello! 👋",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Conversations are end-to-end encrypted.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageList(
    messagesProvider: () -> List<Message>,
    isOtherTypingProvider: () -> Boolean,
    otherUserProvider: () -> com.example.models.UserProfile?,
    currentUserId: String,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val messages = messagesProvider()
    val isOtherTyping = isOtherTypingProvider()
    val otherUser = otherUserProvider()

    val groupedMessages = remember(messages) {
        messages.groupBy { getGroupedDateSeparator(it.timestamp) }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
    ) {
        groupedMessages.forEach { (dateStr, messagesInGroup) ->
            stickyHeader(key = dateStr) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Text(
                            text = dateStr,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            
            itemsIndexed(
                items = messagesInGroup,
                key = { _, msg -> msg.id },
                contentType = { _, _ -> "message" }
            ) { _, msg ->
                val isMe = msg.senderId == currentUserId
                val isSeen = msg.seen
                val isDelivered = remember(otherUser, msg.timestamp) {
                    otherUser?.online == true || otherUser?.onlineStatus == true || (otherUser != null && otherUser.lastSeen > msg.timestamp)
                }

                Box(modifier = Modifier.animateItem()) {
                    MessageBubble(
                        message = msg,
                        isMe = isMe,
                        isSeen = isSeen,
                        isDelivered = isDelivered
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
        item(key = "typing_indicator", contentType = "typing") {
            TypingIndicatorItem(isOtherTyping = isOtherTyping)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    userId: String,
    onBack: () -> Unit
) {
    val chatViewModel: ChatViewModel = viewModel()
    val messages by chatViewModel.messages.collectAsState()
    val otherUser by chatViewModel.otherUser.collectAsState()
    val isOtherTyping by chatViewModel.isOtherTyping.collectAsState()
    
    val currentUserId = try { FirebaseManager.auth.currentUser?.uid ?: "" } catch(e: Exception) { "" }
    val chatId = if (currentUserId < userId) "${currentUserId}_$userId" else "${userId}_$currentUserId"
    
    LaunchedEffect(chatId) {
        chatViewModel.loadMessages(chatId, userId)
    }
    
    DisposableEffect(chatId) {
        com.example.services.ActiveChatSession.activeChatId = chatId
        onDispose {
            com.example.services.ActiveChatSession.activeChatId = null
            chatViewModel.updateTypingStatus(chatId, false)
        }
    }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    val isAtBottomState by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem == null || lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 2
        }
    }
    
    LaunchedEffect(isOtherTyping) {
        if (isOtherTyping && messages.isNotEmpty() && isAtBottomState) {
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && isAtBottomState) {
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.navigationBars
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ChatTopBar(
                    otherUserProvider = { otherUser },
                    isOtherTypingProvider = { isOtherTyping },
                    onBack = onBack
                )
                
                Box(modifier = Modifier.weight(1f)) {
                    EmptyChatPlaceholder(
                        isOtherTypingProvider = { isOtherTyping },
                        messagesEmptyProvider = { messages.isEmpty() }
                    )
                    
                    MessageList(
                        messagesProvider = { messages },
                        isOtherTypingProvider = { isOtherTyping },
                        otherUserProvider = { otherUser },
                        currentUserId = currentUserId,
                        listState = listState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                MessageInputBar(
                    onSend = { text ->
                        chatViewModel.sendMessage(chatId, userId, text)
                    },
                    onTextChange = { text ->
                        chatViewModel.setLocalTyping(chatId, text.isBlank())
                    },
                    modifier = Modifier.fillMaxWidth().imePadding()
                )
            }
        }
    }
}

@Composable
fun AnimatedDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots_transition")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, delayMillis = 140, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, delayMillis = 280, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha3"
    )
    
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).graphicsLayer { alpha = alpha1 })
        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).graphicsLayer { alpha = alpha2 })
        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).graphicsLayer { alpha = alpha3 })
    }
}

@Composable
fun ChatTopBar(
    otherUserProvider: () -> com.example.models.UserProfile?,
    isOtherTypingProvider: () -> Boolean,
    onBack: () -> Unit
) {
    val otherUser = otherUserProvider()
    val isOtherTyping = isOtherTypingProvider()
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val isOnline = otherUser?.online == true || otherUser?.onlineStatus == true
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = textPrimary)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF374151))
                .border(
                    width = if (isOnline) 1.5.dp else 0.dp, 
                    color = Color(0xFF10B981), 
                    shape = CircleShape
                )
        ) {
            if (otherUser?.avatar?.isNotEmpty() == true) {
                AsyncImage(
                    model = otherUser.avatar,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (otherUser != null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_wave_logo),
                        contentDescription = "Default Avatar",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            val displayName = otherUser?.displayName?.takeIf { it.isNotBlank() } ?: otherUser?.username ?: "Loading..."
            Text(displayName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            
            AnimatedContent(
                targetState = isOtherTyping to isOnline,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, delayMillis = 60)) + 
                     slideInVertically(animationSpec = tween(220, delayMillis = 60)) { height -> -height / 2 })
                    .togetherWith(fadeOut(animationSpec = tween(90)) + 
                                  slideOutVertically(animationSpec = tween(90)) { height -> height / 2 })
                },
                label = "status_animation"
            ) { (typing, online) ->
                if (typing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("typing", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(3.dp))
                        AnimatedDots()
                    }
                } else if (online) {
                    Text("Online", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Medium)
                } else {
                    val lastSeen = otherUser?.lastSeen ?: 0L
                    if (lastSeen > 0) {
                        val timeString = getRelativeTimeString(lastSeen)
                        Text("Last active $timeString", fontSize = 12.sp, color = textSecondary)
                    } else {
                        Text("Offline", fontSize = 12.sp, color = textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageInputBar(
    onSend: (String) -> Unit,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.background)
                .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    onTextChange(it)
                },
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                        onTextChange("")
                    }
                }),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text("Message...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 16.sp)
                        }
                        innerTextField()
                    }
                }
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        val primaryGradient = Brush.linearGradient(
            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
        )
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(primaryGradient)
                .clickable {
                    if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                        onTextChange("")
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun MessageBubble(
    message: Message, 
    isMe: Boolean, 
    isSeen: Boolean,
    isDelivered: Boolean
) {
    val timeString = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }
    
    val bubbleShape = remember(isMe) {
        if (isMe) {
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 2.dp)
        } else {
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 2.dp, bottomEnd = 18.dp)
        }
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface

    val bubbleModifier = remember(isMe, primaryColor, secondaryColor, surfaceColor) {
        if (isMe) {
            Modifier
                .clip(bubbleShape)
                .background(Brush.linearGradient(listOf(primaryColor, secondaryColor)))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        } else {
            Modifier
                .clip(bubbleShape)
                .background(surfaceColor)
                .border(1.dp, Color.White.copy(alpha = 0.04f), bubbleShape)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        }
    }
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(modifier = bubbleModifier) {
                Text(
                    text = message.text, 
                    color = Color.White, 
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = timeString, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
                    fontSize = 11.sp
                )
                if (isMe) {
                    Spacer(modifier = Modifier.width(3.dp))
                    val statusText = when {
                        isSeen -> "✓✓"
                        isDelivered -> "✓✓"
                        else -> "✓"
                    }
                    val statusColor = if (isSeen) Color(0xFF06B6D4) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    Text(
                        text = statusText, 
                        color = statusColor, 
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TypingBubbleContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots_transition")
    val t1 by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t1"
    )
    val t2 by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, delayMillis = 150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t2"
    )
    val t3 by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, delayMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = t1 * density }
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = t2 * density }
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        )
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = t3 * density }
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        )
    }
}

@Composable
fun TypingIndicatorItem(isOtherTyping: Boolean) {
    androidx.compose.animation.AnimatedVisibility(
        visible = isOtherTyping,
        enter = fadeIn(animationSpec = tween(150)) + expandVertically(animationSpec = tween(150)),
        exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = tween(150))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.04f),
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                TypingBubbleContent()
            }
        }
    }
}
