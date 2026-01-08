package com.refreshme.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    fun getChatMessages(stylistId: String) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("chats")
            .document(getChatId(userId, stylistId))
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    _messages.value = it.toObjects(ChatMessage::class.java)
                }
            }
    }

    fun sendMessage(stylistId: String, text: String) {
        val userId = auth.currentUser?.uid ?: return
        val message = ChatMessage(userId, stylistId, text)

        firestore.collection("chats")
            .document(getChatId(userId, stylistId))
            .collection("messages")
            .add(message)
    }

    private fun getChatId(userId: String, stylistId: String): String {
        return if (userId < stylistId) {
            "${userId}_${stylistId}"
        } else {
            "${stylistId}_${userId}"
        }
    }
}
