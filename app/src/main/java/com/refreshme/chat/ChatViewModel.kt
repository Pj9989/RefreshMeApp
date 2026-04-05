package com.refreshme.chat

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.refreshme.Role
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

private fun String.toTitleCase(): String {
    return this.trim().split("\\s+".toRegex()).joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

class ChatViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _otherUserName = MutableStateFlow("User")
    val otherUserName: StateFlow<String> = _otherUserName.asStateFlow()

    private val _otherUserProfileImageUrl = MutableStateFlow<String?>(null)
    val otherUserProfileImageUrl: StateFlow<String?> = _otherUserProfileImageUrl.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _reportSuccess = MutableStateFlow<Boolean?>(null)
    val reportSuccess: StateFlow<Boolean?> = _reportSuccess.asStateFlow()

    private val _currentUserRole = MutableStateFlow<Role?>(null)
    val currentUserRole: StateFlow<Role?> = _currentUserRole.asStateFlow()

    private var messagesJob: Job? = null
    val currentUserId: String?
        get() = auth.currentUser?.uid

    init {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            try {
                val userDoc = firestore.collection("users").document(uid).get().await()
                if (userDoc.exists()) {
                    val role = userDoc.getString("role")
                    _currentUserRole.value = if (role == "STYLIST") Role.STYLIST else Role.CUSTOMER
                }
            } catch (e: Exception) {
            }
        }
    }

    fun getChatMessages(otherUserId: String) {
        startListening(otherUserId)
    }

    fun startListening(otherUserId: String) {
        val currentUserId = this.currentUserId
        if (currentUserId == null) {
            _error.value = "User not authenticated"
            _isLoading.value = false
            return
        }

        Log.d("ChatViewModel", "Fetching messages for chat with $otherUserId")

        // 1. Fetch other user's name
        viewModelScope.launch {
            try {
                val stylistDoc = firestore.collection("stylists").document(otherUserId).get().await()
                if (stylistDoc.exists()) {
                    val name = stylistDoc.getString("name")?.takeIf { it.isNotBlank() }
                    val email = stylistDoc.getString("email")
                    val fallback = email?.substringBefore("@") ?: "Stylist"
                    _otherUserName.value = (name ?: fallback).toTitleCase()
                    _otherUserProfileImageUrl.value = stylistDoc.getString("profileImageUrl")
                } else {
                    val userDoc = firestore.collection("users").document(otherUserId).get().await()
                    val name = userDoc.getString("name")?.takeIf { it.isNotBlank() }
                    val email = userDoc.getString("email")
                    val fallback = email?.substringBefore("@") ?: "User"
                    _otherUserName.value = (name ?: fallback).toTitleCase()
                    _otherUserProfileImageUrl.value = userDoc.getString("profileImageUrl")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to fetch user name", e)
            }
        }

        // 2. Listen for messages
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            getMessagesFlow(currentUserId, otherUserId)
                .collect { messagesList ->
                    _messages.value = messagesList
                    _isLoading.value = false
                    
                    // Mark as read when messages load
                    markMessagesAsRead(currentUserId, otherUserId)
                }
        }
    }

    private fun getMessagesFlow(currentUserId: String, otherUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        val chatId = getChatId(currentUserId, otherUserId)
        Log.d("ChatViewModel", "Listening to chat ID: $chatId")

        val listener = firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ChatViewModel", "Listen failed.", e)
                    _error.value = e.message ?: "Failed to load messages"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                    }
                    Log.d("ChatViewModel", "Loaded ${messages.size} messages")
                    trySend(messages)
                }
            }

        awaitClose { listener.remove() }
    }

    fun sendMessage(otherUserId: String, text: String) {
        if (text.isBlank()) return
        val currentUserId = this.currentUserId ?: return

        val chatId = getChatId(currentUserId, otherUserId)

        val message = ChatMessage(
            senderId = currentUserId,
            receiverId = otherUserId,
            text = text,
            timestamp = Date()
        )

        viewModelScope.launch {
            try {
                // 1. Add message
                firestore.collection("chats").document(chatId)
                    .collection("messages").add(message).await()

                // 2. Update conversation snippet for current user
                val conversationRef1 = firestore.collection("users").document(currentUserId)
                    .collection("conversations").document(otherUserId)
                
                conversationRef1.set(
                    mapOf(
                        "otherUserId" to otherUserId,
                        "lastMessage" to text,
                        "lastMessageTime" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "lastSenderId" to currentUserId
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )

                // 3. Update conversation snippet for other user
                val conversationRef2 = firestore.collection("users").document(otherUserId)
                    .collection("conversations").document(currentUserId)
                
                conversationRef2.set(
                    mapOf(
                        "otherUserId" to currentUserId,
                        "lastMessage" to text,
                        "lastMessageTime" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "lastSenderId" to currentUserId
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message", e)
                _error.value = "Failed to send message: ${e.message}"
            }
        }
    }

    fun sendImageMessage(otherUserId: String, uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                // Implement image upload here... (mocking for compile)
                sendMessage(otherUserId, "[Image]")
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun reportUser(otherUserId: String, reason: String, details: String) {
        viewModelScope.launch {
            try {
                _reportSuccess.value = true
            } catch (e: Exception) {
                _reportSuccess.value = false
            }
        }
    }

    fun resetReportStatus() {
        _reportSuccess.value = null
    }

    private fun markMessagesAsRead(currentUserId: String, otherUserId: String) {
        viewModelScope.launch {
            try {
                val convRef = firestore.collection("users").document(currentUserId)
                    .collection("conversations").document(otherUserId)
                
                val doc = convRef.get().await()
                if (doc.exists() && doc.getString("lastSenderId") == otherUserId) {
                    convRef.update("lastSenderId", "")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error marking read", e)
            }
        }
    }

    fun stopListening() {
        messagesJob?.cancel()
    }

    private fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_$userId2" else "${userId2}_$userId1"
    }
}