package com.refreshme.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.R
import com.refreshme.data.Message

class ChatFragment : Fragment() {

    private val args: ChatFragmentArgs by navArgs()
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<Message>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        messagesRecyclerView = view.findViewById(R.id.messages_recycler_view)
        messageEditText = view.findViewById(R.id.message_edit_text)
        sendButton = view.findViewById(R.id.send_button)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatAdapter = ChatAdapter(messages)
        messagesRecyclerView.layoutManager = LinearLayoutManager(context)
        messagesRecyclerView.adapter = chatAdapter

        fetchMessages()

        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun fetchMessages() {
        firestore.collection("conversations").document(args.conversationId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                messages.clear()
                snapshots?.forEach { document ->
                    val message = document.toObject(Message::class.java).copy(id = document.id)
                    messages.add(message)
                }
                chatAdapter.notifyDataSetChanged()
            }
    }

    private fun sendMessage() {
        val text = messageEditText.text.toString()
        if (text.isNotBlank()) {
            val message = Message(
                senderId = auth.currentUser?.uid ?: "",
                text = text,
                timestamp = System.currentTimeMillis()
            )
            firestore.collection("conversations").document(args.conversationId)
                .collection("messages")
                .add(message)
            messageEditText.text.clear()
        }
    }
}