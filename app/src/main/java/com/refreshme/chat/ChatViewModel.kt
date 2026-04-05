package com.refreshme.chat

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.refreshme.util.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _otherUserName = MutableStateFlow("User")
    val otherUserName: StateFlow<String> = _otherUserName.asStateFlow()
    
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage

    private var messagesJob: Job? = null

    fun getChatMessages(otherUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        Log.d("ChatViewModel", "Fetching messages for chat with $otherUserId")

        // 1. Fetch other user's name
        viewModelScope.launch {
            try {
                val stylistDoc = firestore.collection("stylists").document(otherUserId).get().await()
                if (stylistDoc.exists()) {
                    _otherUserName.value = stylistDoc.getString("name") ?: "Stylist"
                } else {
                    val userDoc = firestore.collection("users").document(otherUserId).get().await()
                    _otherUserName.value = userDoc.getString("name") ?: "User"
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to fetch user name", e)
            }
        }

        // 2. Listen for messages
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            getMessagesFlow(currentUserId, otherUserId)
                .catch { e -> Log.e("ChatViewModel", "Failed to listen for messages", e) }
                .collect { messageList ->
                    _messages.value = messageList
                    markMessagesAsRead(currentUserId, otherUserId, messageList)
                }
        }
    }

    private fun getMessagesFlow(currentUserId: String, otherUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        val chatId = getChatId(currentUserId, otherUserId)
        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                
                val messageList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(messageList)
            }
            
        awaitClose { listener.remove() }
    }

    private fun markMessagesAsRead(currentUserId: String, otherUserId: String, messages: List<ChatMessage>) {
        val unreadMessages = messages.filter { it.receiverId == currentUserId && !it.read }
        if (unreadMessages.isEmpty()) return

        val chatId = getChatId(currentUserId, otherUserId)
        viewModelScope.launch {
            try {
                val batch = firestore.batch()
                unreadMessages.forEach { msg ->
                    val msgRef = firestore.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .document(msg.id)
                    batch.update(msgRef, "read", true)
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to mark messages as read", e)
            }
        }
    }

    fun sendMessage(otherUserId: String, text: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val message = ChatMessage(
            senderId = currentUserId,
            receiverId = otherUserId,
            text = text,
            type = MessageType.TEXT,
            timestamp = Date(),
            read = false
        )

        val chatId = getChatId(currentUserId, otherUserId)
        
        viewModelScope.launch {
            try {
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .add(message).await()
                    
                updateChatSummary(chatId, currentUserId, otherUserId, text)
            } catch (e: Exception) {
                _toastMessage.emit("Failed to send: ${e.message}")
            }
        }
    }

    fun sendImageMessage(otherUserId: String, uri: Uri) {
        val currentUserId = auth.currentUser?.uid ?: return
        _isUploading.value = true
        
        viewModelScope.launch {
            try {
                val fileName = "chat_${UUID.randomUUID()}.jpg"
                val storageRef = storage.reference.child("chat_images/$fileName")
                
                storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                
                val message = ChatMessage(
                    senderId = currentUserId,
                    receiverId = otherUserId,
                    imageUrl = downloadUrl,
                    type = MessageType.IMAGE,
                    timestamp = Date(),
                    read = false
                )
                
                val chatId = getChatId(currentUserId, otherUserId)
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .add(message).await()
                    
                updateChatSummary(chatId, currentUserId, otherUserId, "Sent an image")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error uploading image", e)
                _toastMessage.emit("Image upload failed: ${e.message}")
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun shareStyleProfile(otherUserId: String) {
        viewModelScope.launch {
            try {
                val user = UserManager.getCurrentUser()
                val profile = user?.styleProfile ?: run {
                    _toastMessage.emit("Complete your AI Style Quiz first!")
                    return@launch
                }
                
                val currentUserId = auth.currentUser?.uid ?: return@launch
                val metadata = mapOf(
                    "vibe" to profile.vibe,
                    "gender" to profile.gender,
                    "frequency" to profile.frequency,
                    "finish" to profile.finish
                )
                
                val message = ChatMessage(
                    senderId = currentUserId,
                    receiverId = otherUserId,
                    text = "Shared my AI Style Profile",
                    type = MessageType.STYLE_PROFILE,
                    metadata = metadata,
                    timestamp = Date(),
                    read = false
                )

                val chatId = getChatId(currentUserId, otherUserId)
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .add(message).await()
                    
                updateChatSummary(chatId, currentUserId, otherUserId, "Shared AI Style Profile")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to share profile", e)
                _toastMessage.emit("Failed to share profile")
            }
        }
    }

    private suspend fun updateChatSummary(chatId: String, currentUserId: String, otherUserId: String, lastMessageText: String) {
        val lastMessageData = mapOf(
            "lastMessage" to lastMessageText,
            "lastMessageTime" to Date(),
            "participants" to listOf(currentUserId, otherUserId),
            "lastSenderId" to currentUserId // Track who sent the last message for unread indicators in list
        )
        firestore.collection("chats").document(chatId).set(lastMessageData, SetOptions.merge()).await()
    }

    private fun getChatId(userId: String, otherId: String): String {
        return if (userId < otherId) "${userId}_${otherId}" else "${otherId}_${userId}"
    }
}