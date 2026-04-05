package com.refreshme.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.refreshme.data.Conversation
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private fun String.toTitleCase(): String {
    return this.trim().split("\\s+".toRegex()).joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

data class MessagesUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = true,
    val isAuthenticated: Boolean = true
)

class MessagesViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null
    val currentUserId: String?
        get() = auth.currentUser?.uid

    init {
        startListening()
    }

    private fun startListening() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            _uiState.value = MessagesUiState(isAuthenticated = false, isLoading = false)
            return
        }

        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            getConversationsFlow(currentUserId).collect { conversations ->
                // Fetch other user's name and image for each conversation
                val detailedConversations = conversations.map { fetchUserDetails(it) }
                _uiState.value = MessagesUiState(
                    conversations = detailedConversations.sortedByDescending { it.lastMessageTime },
                    isLoading = false
                )
            }
        }
    }

    private fun getConversationsFlow(currentUserId: String): Flow<List<Conversation>> = callbackFlow {
        val conversationsRef = firestore.collection("users").document(currentUserId)
            .collection("conversations")
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)

        val listener = conversationsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("MessagesViewModel", "Listen failed.", e)
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val conversations = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                }
                trySend(conversations)
            }
        }

        awaitClose { listener.remove() }
    }

    private suspend fun fetchUserDetails(conversation: Conversation): Conversation {
        return try {
            val stylistDoc = firestore.collection("stylists").document(conversation.otherUserId).get().await()
            if (stylistDoc.exists()) {
                val name = stylistDoc.getString("name")?.takeIf { it.isNotBlank() }
                val email = stylistDoc.getString("email")
                val fallback = email?.substringBefore("@") ?: "Stylist"
                conversation.copy(
                    otherUserName = (name ?: fallback).toTitleCase(),
                    otherUserProfileImageUrl = stylistDoc.getString("profileImageUrl") ?: ""
                )
            } else {
                val userDoc = firestore.collection("users").document(conversation.otherUserId).get().await()
                val name = userDoc.getString("name")?.takeIf { it.isNotBlank() }
                val email = userDoc.getString("email")
                val fallback = email?.substringBefore("@") ?: "User"
                conversation.copy(
                    otherUserName = (name ?: fallback).toTitleCase(),
                    otherUserProfileImageUrl = userDoc.getString("profileImageUrl") ?: ""
                )
            }
        } catch (e: Exception) {
            Log.w("MessagesViewModel", "Failed to fetch user details for ${conversation.otherUserId}", e)
            conversation
        }
    }

    fun stopListening() {
        messagesJob?.cancel()
    }
}