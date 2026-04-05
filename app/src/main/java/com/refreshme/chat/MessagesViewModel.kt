package com.refreshme.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.data.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class MessagesUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = true,
    val isAuthenticated: Boolean = true
)

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    val currentUserId: String? get() = auth.currentUser?.uid

    init {
        setupAuthStateListener()
    }

    private fun setupAuthStateListener() {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            _uiState.value = _uiState.value.copy(isAuthenticated = currentUser != null)
            
            if (currentUser != null) {
                observeConversations(currentUser.uid)
            } else {
                _uiState.value = _uiState.value.copy(conversations = emptyList(), isLoading = false)
            }
        }
        auth.addAuthStateListener(listener)
        authStateListener = listener
    }

    private fun observeConversations(currentUserId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            getConversationsFlow(currentUserId)
                .catch { e ->
                    Log.e("MessagesViewModel", "Error fetching conversations", e)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .collect { rawConversations ->
                    // Sort descending by last message time (newest at top), handling nulls gracefully
                    val sortedRaw = rawConversations.sortedByDescending { it.lastMessageTime?.time ?: 0L }
                    
                    val updatedConversations = sortedRaw.map { conv ->
                        fetchUserDetails(conv)
                    }
                    _uiState.value = _uiState.value.copy(
                        conversations = updatedConversations,
                        isLoading = false
                    )
                }
        }
    }

    private fun getConversationsFlow(currentUserId: String): Flow<List<Conversation>> = callbackFlow {
        val listener = firestore.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }

                val conversations = snapshots?.mapNotNull { document ->
                    val conversation = document.toObject(Conversation::class.java).copy(id = document.id)
                    val otherUserId = conversation.participants.firstOrNull { it != currentUserId }
                    if (otherUserId != null) {
                        conversation.copy(otherUserId = otherUserId)
                    } else null
                } ?: emptyList()

                trySend(conversations)
            }

        awaitClose { listener.remove() }
    }

    private suspend fun fetchUserDetails(conversation: Conversation): Conversation {
        return try {
            val stylistDoc = firestore.collection("stylists").document(conversation.otherUserId).get().await()
            if (stylistDoc.exists()) {
                conversation.copy(
                    otherUserName = stylistDoc.getString("name") ?: "Stylist",
                    otherUserProfileImageUrl = stylistDoc.getString("profileImageUrl") ?: ""
                )
            } else {
                val userDoc = firestore.collection("users").document(conversation.otherUserId).get().await()
                conversation.copy(
                    otherUserName = userDoc.getString("name") ?: "User",
                    otherUserProfileImageUrl = userDoc.getString("profileImageUrl") ?: ""
                )
            }
        } catch (e: Exception) {
            Log.w("MessagesViewModel", "Failed to fetch user details for ${conversation.otherUserId}", e)
            conversation
        }
    }

    override fun onCleared() {
        super.onCleared()
        authStateListener?.let { auth.removeAuthStateListener(it) }
    }
}