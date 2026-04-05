package com.refreshme.chat

import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    otherUserId: String,
    currentUserId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val otherUserName by viewModel.otherUserName.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    var newMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.sendImageMessage(otherUserId, it) }
    }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(otherUserId) {
        viewModel.getChatMessages(otherUserId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(otherUserName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Online", fontSize = 12.sp, color = Color(0xFF4CAF50))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Column {
                if (isUploading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                ChatInputBar(
                    message = newMessage,
                    onMessageChange = { newMessage = it },
                    onSendClick = {
                        if (newMessage.isNotBlank()) {
                            viewModel.sendMessage(otherUserId, newMessage)
                            newMessage = ""
                        }
                    },
                    onShareProfileClick = { viewModel.shareStyleProfile(otherUserId) },
                    onPickImageClick = { launcher.launch("image/*") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        isMyMessage = message.senderId == currentUserId
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, isMyMessage: Boolean) {
    val bubbleColor = if (isMyMessage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMyMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    
    val shape = if (isMyMessage) {
        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
    ) {
        when (message.type) {
            MessageType.STYLE_PROFILE -> StyleProfileCard(message, isMyMessage)
            MessageType.IMAGE -> ImageMessageCard(message, isMyMessage)
            else -> {
                Surface(
                    color = bubbleColor,
                    shape = shape,
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(text = message.text, color = textColor, fontSize = 15.sp)
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                        ) {
                            Text(
                                text = formatRelativeTime(message.timestamp),
                                fontSize = 10.sp,
                                color = textColor.copy(alpha = 0.6f)
                            )
                            if (isMyMessage) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (message.read) Icons.Default.DoneAll else Icons.Default.Done,
                                    contentDescription = null,
                                    tint = textColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageMessageCard(message: ChatMessage, isMyMessage: Boolean) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(message.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Image message",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentScale = ContentScale.Crop
            )
            
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                color = Color.Black.copy(alpha = 0.6f),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatRelativeTime(message.timestamp),
                        color = Color.White,
                        fontSize = 10.sp
                    )
                    if (isMyMessage) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.read) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StyleProfileCard(message: ChatMessage, isMyMessage: Boolean) {
    val metadata = message.metadata ?: return
    
    Card(
        modifier = Modifier
            .width(260.dp)
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMyMessage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("AI Style Profile", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            
            Spacer(Modifier.height(12.dp))
            
            ProfileRow("Vibe", metadata["vibe"] ?: "Unknown")
            ProfileRow("Gender", metadata["gender"] ?: "Unknown")
            ProfileRow("Frequency", metadata["frequency"] ?: "Unknown")
            ProfileRow("Finish", metadata["finish"] ?: "Unknown")
            
            Spacer(Modifier.height(12.dp))
            Text(
                "SHARED VIA REFRESHME AI • ${formatRelativeTime(message.timestamp)}", 
                fontSize = 9.sp, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ChatInputBar(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onShareProfileClick: () -> Unit,
    onPickImageClick: () -> Unit
) {
    val inputColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    
    Surface(
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPickImageClick) {
                Icon(
                    Icons.Default.AddPhotoAlternate, 
                    contentDescription = "Send Photo",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onShareProfileClick) {
                Icon(
                    Icons.Default.AutoAwesome, 
                    contentDescription = "Share Style Profile",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            TextField(
                value = message,
                onValueChange = onMessageChange,
                placeholder = { Text("Message...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = inputColor,
                    unfocusedContainerColor = inputColor
                ),
                maxLines = 4
            )
            
            Spacer(Modifier.width(8.dp))
            
            FilledIconButton(
                onClick = onSendClick,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
            }
        }
    }
}

fun formatRelativeTime(date: Date?): String {
    if (date == null) return ""
    return DateUtils.getRelativeTimeSpanString(
        date.time,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}