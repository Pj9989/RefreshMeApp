package com.refreshme.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.refreshme.R
import com.refreshme.data.Conversation

class ConversationAdapter(
    private val conversations: List<Conversation>,
    private val onConversationClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.profile_image)
        val userName: TextView = view.findViewById(R.id.user_name)
        val lastMessage: TextView = view.findViewById(R.id.last_message)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = conversations[position]
        holder.userName.text = conversation.otherUserName
        holder.lastMessage.text = conversation.lastMessage
        Glide.with(holder.itemView.context)
            .load(conversation.otherUserProfileImageUrl)
            .circleCrop()
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.profileImage)
        holder.itemView.setOnClickListener { onConversationClick(conversation) }
    }

    override fun getItemCount() = conversations.size
}