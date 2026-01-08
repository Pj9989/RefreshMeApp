package com.refreshme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshme.ui.theme.RefreshMeTheme
import kotlinx.coroutines.launch

enum class MessageType {
    USER, APPOINTMENT, CANCELLATION, PAYMENT
}

data class Message(
    val sender: String,
    val text: String,
    val timestamp: String,
    val profilePic: Int,
    val type: MessageType = MessageType.USER
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen() {
    val messages = remember {
        mutableStateListOf(
            Message("Sarah Wilson", "Hey, how are you?", "10:00 AM", R.drawable.ic_launcher_foreground),
            Message("You", "I'm good, thanks! How about you?", "10:01 AM", R.drawable.ic_launcher_foreground),
            Message("System", "Appointment Confirmed: Haircut with Sarah, Dec 21, 2023 at 2:00 PM.", "10:05 AM", R.drawable.ic_launcher_foreground, MessageType.APPOINTMENT),
            Message("Sarah Wilson", "Great! See you then.", "10:06 AM", R.drawable.ic_launcher_foreground),
            Message("System", "Payment Successful: $50 for Haircut.", "10:07 AM", R.drawable.ic_launcher_foreground, MessageType.PAYMENT),
            Message("System", "Appointment Cancelled by client.", "10:08 AM", R.drawable.ic_launcher_foreground, MessageType.CANCELLATION),
            )
    }
    var newMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sarah Wilson")
                        Text(
                            "Online",
                            fontSize = 12.sp,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier.shadow(4.dp)
            )
        },
        bottomBar = {
            ChatBox(
                message = newMessage,
                onMessageChange = { newMessage = it },
                onSend = {
                    if (newMessage.isNotBlank()) {
                        messages.add(Message("You", newMessage, "10:04 AM", R.drawable.ic_launcher_foreground))
                        newMessage = ""
                        coroutineScope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding() + 16.dp, bottom = paddingValues.calculateBottomPadding() + 16.dp, start = 16.dp, end = 16.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                when (message.type) {
                    MessageType.USER -> MessageCard(message)
                    else -> SystemMessageCard(message)
                }
            }
        }
    }
}

@Composable
fun MessageCard(message: Message) {
    val isSender = message.sender == "You"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSender) Arrangement.End else Arrangement.Start
    ) {
        if (!isSender) {
            Image(
                painter = painterResource(id = message.profilePic),
                contentDescription = message.sender,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .align(Alignment.Bottom)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isSender) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
                    .background(
                        if (isSender) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isSender) 16.dp else 0.dp,
                            bottomEnd = if (isSender) 0.dp else 16.dp
                        )
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (isSender) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SystemMessageCard(message: Message) {
    val icon: ImageVector
    val backgroundColor: Color
    val contentColor: Color
    val boldText: String

    when (message.type) {
        MessageType.APPOINTMENT -> {
            icon = Icons.Default.Event
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            boldText = "Confirmed"
        }
        MessageType.CANCELLATION -> {
            icon = Icons.Default.Cancel
            backgroundColor = MaterialTheme.colorScheme.errorContainer
            contentColor = MaterialTheme.colorScheme.onErrorContainer
            boldText = "Cancelled"
        }
        MessageType.PAYMENT -> {
            icon = Icons.Default.Payment
            backgroundColor = Color(0xFFD4AF37) // Muted Gold
            contentColor = Color.Black
            boldText = "Successful"
        }
        else -> return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(backgroundColor, shape = RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(message.text.substringBefore(":"))
                    }
                    append(message.text.substringAfter(":"))
                },
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message.timestamp,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), thickness = 1.dp)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBox(
    message: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        tonalElevation = 4.dp,
        modifier = Modifier.shadow(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                shape = CircleShape,
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = message.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send"
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessagesScreenPreview() {
    RefreshMeTheme(darkTheme = false) {
        MessagesScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun MessagesScreenDarkPreview() {
    RefreshMeTheme(darkTheme = true) {
        MessagesScreen()
    }
}